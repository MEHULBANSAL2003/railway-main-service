package com.railway.main_service.service.journeyService;

import com.railway.common.exceptions.BaseException;
import com.railway.main_service.dto.request.journey.AddJourneyRequest;
import com.railway.main_service.dto.request.journey.CancelJourneyRequest;
import com.railway.main_service.dto.response.journey.BulkGenerateResponse;
import com.railway.main_service.dto.response.journey.JourneyResponse;
import com.railway.main_service.entity.*;
import com.railway.main_service.enums.JourneyStatus;
import com.railway.main_service.enums.RunDay;
import com.railway.main_service.repository.*;
import com.railway.main_service.service.inventoryService.InventoryInitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class JourneyServiceImpl implements JourneyService {

  private static final int BOOKING_WINDOW_DAYS = 120;
  private static final DateTimeFormatter DT_FMT =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final JourneyRepository       journeyRepository;
  private final TrainRepository         trainRepository;
  private final TrainScheduleRepository scheduleRepository;
  private final TrainStopRepository     trainStopRepository;
  private final TrainCoachRepository    trainCoachRepository;
  private final InventoryInitService inventoryInitService;

  // ── Paginated list — all history, filtered ────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<JourneyResponse> getJourneysForTrain(
    String trainNumber, int page, int size,
    String sortBy, String sortDir,
    LocalDate dateFrom, LocalDate dateTo,
    List<String> statuses) {

    TrainEntity train = findTrain(trainNumber);

    Sort sort = sortDir.equalsIgnoreCase("asc")
      ? Sort.by(sortBy).ascending()
      : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);

    LocalTime srcDep = trainStopRepository
      .findSourceStop(train.getTrainId())
      .map(TrainStopEntity::getDepartureTime)
      .orElse(null);

    Page<JourneyEntity> raw = journeyRepository
      .findFiltered(train.getTrainId(), dateFrom, dateTo, pageable);

    // Derive status in memory, then filter if statuses requested
    List<JourneyResponse> mapped = raw.getContent().stream()
      .map(j -> toResponse(j, srcDep))
      .collect(Collectors.toList());

    if (statuses != null && !statuses.isEmpty()) {
      Set<String> statusSet = new HashSet<>(statuses);
      mapped = mapped.stream()
        .filter(j -> statusSet.contains(j.getStatus()))
        .collect(Collectors.toList());
    }

    return new PageImpl<>(mapped, pageable, raw.getTotalElements());
  }

  // ── Bulk generate ─────────────────────────────────────────────────────────

  @Override
  @Transactional
  public BulkGenerateResponse bulkGenerate(String trainNumber) {
    TrainEntity train = findTrain(trainNumber);
    validateTrainReady(train);

    TrainScheduleEntity schedule = findActiveSchedule(train);
    Set<RunDay> runDays = schedule.getRunDaysAsSet();

    LocalDate from = LocalDate.now().plusDays(1);
    LocalDate to   = LocalDate.now().plusDays(BOOKING_WINDOW_DAYS);

    List<LocalDate> createdDates = new ArrayList<>();
    int skipped = 0;

    LocalDate cursor = from;
    while (!cursor.isAfter(to)) {
      String dayName = cursor.getDayOfWeek().name().substring(0, 3);
      boolean isScheduledDay;
      try { isScheduledDay = runDays.contains(RunDay.valueOf(dayName)); }
      catch (IllegalArgumentException e) { isScheduledDay = false; }

      if (isScheduledDay &&
        !journeyRepository.existsByTrain_TrainIdAndJourneyDate(train.getTrainId(), cursor)) {
        JourneyEntity j = JourneyEntity.builder()
          .train(train).schedule(schedule).journeyDate(cursor)
          .isCancelled(false).chartPrepared(false).build();
        JourneyEntity saved = journeyRepository.save(j);
      inventoryInitService.initForJourney(saved);
      createdDates.add(cursor);
      } else { skipped++; }
      cursor = cursor.plusDays(1);
    }

    log.info("Bulk generate for train {} — created={} skipped={}", trainNumber, createdDates.size(), skipped);
    return BulkGenerateResponse.builder()
      .created(createdDates.size()).skipped(skipped).total(BOOKING_WINDOW_DAYS)
      .from(from).to(to).createdDates(createdDates).build();
  }

  // ── Single generate ───────────────────────────────────────────────────────

  @Override
  @Transactional
  public JourneyResponse generateForTrain(String trainNumber) {
    TrainEntity train = findTrain(trainNumber);
    return createJourneyForDate(train, LocalDate.now().plusDays(BOOKING_WINDOW_DAYS), "generate");
  }

  // ── Manual add ────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public JourneyResponse addJourney(String trainNumber, AddJourneyRequest request) {
    return createJourneyForDate(findTrain(trainNumber), request.getJourneyDate(), "manual");
  }

  // ── Cancel ────────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public void cancelJourney(String trainNumber, Long journeyId, CancelJourneyRequest request) {
    TrainEntity train = findTrain(trainNumber);

    JourneyEntity journey = journeyRepository.findById(journeyId)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "JOURNEY_NOT_FOUND",
        "Journey not found: " + journeyId));

    if (!journey.getTrain().getTrainId().equals(train.getTrainId()))
      throw new BaseException(HttpStatus.BAD_REQUEST, "JOURNEY_TRAIN_MISMATCH",
        "Journey does not belong to train " + trainNumber);

    if (Boolean.TRUE.equals(journey.getIsCancelled()))
      throw new BaseException(HttpStatus.BAD_REQUEST, "JOURNEY_ALREADY_CANCELLED",
        "Journey is already cancelled");

    if (Boolean.TRUE.equals(journey.getChartPrepared()))
      throw new BaseException(HttpStatus.BAD_REQUEST, "CHART_ALREADY_PREPARED",
        "Cannot cancel — chart is already prepared");

    LocalTime srcDep = trainStopRepository.findSourceStop(train.getTrainId())
      .map(TrainStopEntity::getDepartureTime).orElse(null);

    JourneyStatus status = journey.deriveStatus(srcDep);
    if (status == JourneyStatus.DEPARTED)
      throw new BaseException(HttpStatus.BAD_REQUEST, "JOURNEY_ALREADY_DEPARTED",
        "Cannot cancel a journey that has already departed");
    if (status == JourneyStatus.COMPLETED)
      throw new BaseException(HttpStatus.BAD_REQUEST, "JOURNEY_ALREADY_COMPLETED",
        "Cannot cancel a journey that is already completed");

    journeyRepository.cancelJourney(journeyId, request.getReason());
    log.info("Journey {} cancelled for train {}. Reason: {}", journeyId, trainNumber, request.getReason());
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private JourneyResponse createJourneyForDate(TrainEntity train, LocalDate date, String source) {
    validateTrainReady(train);

    if (journeyRepository.existsByTrain_TrainIdAndJourneyDate(train.getTrainId(), date))
      throw new BaseException(HttpStatus.CONFLICT, "JOURNEY_ALREADY_EXISTS",
        "Journey already exists for train " + train.getTrainNumber() + " on " + date);

    TrainScheduleEntity schedule = findActiveSchedule(train);

    if (!runsOnDate(schedule, date))
      throw new BaseException(HttpStatus.BAD_REQUEST, "SCHEDULE_DAY_MISMATCH",
        "Train " + train.getTrainNumber() + " does not run on " +
          date.getDayOfWeek().name() + " (" + schedule.getRunsOnDays() + ")");

    JourneyEntity saved = journeyRepository.save(
      JourneyEntity.builder().train(train).schedule(schedule)
        .journeyDate(date).isCancelled(false).chartPrepared(false).build()
    );
    log.info("Journey created [{}] for train {} on {}", source, train.getTrainNumber(), date);

    inventoryInitService.initForJourney(saved);

    LocalTime srcDep = trainStopRepository.findSourceStop(train.getTrainId())
      .map(TrainStopEntity::getDepartureTime).orElse(null);
    return toResponse(saved, srcDep);
  }

  private void validateTrainReady(TrainEntity train) {
    if (trainStopRepository.countByTrain_TrainId(train.getTrainId()) < 2)
      throw new BaseException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_STOPS",
        "Train " + train.getTrainNumber() + " must have at least 2 stops");
    if (trainCoachRepository.countByTrain_TrainIdAndIsActiveTrue(train.getTrainId()) == 0)
      throw new BaseException(HttpStatus.BAD_REQUEST, "NO_ACTIVE_COACHES",
        "Train " + train.getTrainNumber() + " must have at least one active coach");
  }

  private TrainScheduleEntity findActiveSchedule(TrainEntity train) {
    return scheduleRepository.findRunning(train.getTrainId(), LocalDate.now())
      .orElseGet(() -> scheduleRepository.findUpcoming(train.getTrainId(), LocalDate.now())
        .stream().findFirst()
        .orElseThrow(() -> new BaseException(HttpStatus.BAD_REQUEST, "NO_ACTIVE_SCHEDULE",
          "No active schedule found for train " + train.getTrainNumber())));
  }

  private TrainEntity findTrain(String trainNumber) {
    return trainRepository.findByTrainNumber(trainNumber)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_NOT_FOUND",
        "Train not found: " + trainNumber));
  }

  private boolean runsOnDate(TrainScheduleEntity schedule, LocalDate date) {
    if (date.isBefore(schedule.getStartDate())) return false;
    if (schedule.getEndDate() != null && date.isAfter(schedule.getEndDate())) return false;
    try { return schedule.getRunDaysAsSet().contains(RunDay.valueOf(date.getDayOfWeek().name().substring(0, 3))); }
    catch (IllegalArgumentException e) { return false; }
  }

  private JourneyResponse toResponse(JourneyEntity j, LocalTime srcDep) {
    return JourneyResponse.builder()
      .journeyId(j.getJourneyId())
      .journeyDate(j.getJourneyDate())
      .status(j.deriveStatus(srcDep).name())
      .chartPrepared(Boolean.TRUE.equals(j.getChartPrepared()))
      .cancelled(Boolean.TRUE.equals(j.getIsCancelled()))
      .cancelReason(j.getCancelReason())
      .scheduleId(j.getSchedule().getScheduleId())
      .scheduleRunDays(j.getSchedule().getRunsOnDays())
      .createdAt(j.getCreatedAt() != null ? j.getCreatedAt().format(DT_FMT) : null)
      .build();
  }
}
