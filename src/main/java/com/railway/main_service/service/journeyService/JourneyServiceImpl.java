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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

  // ── Bulk generate — all missing journeys for next 120 days ────────────────

  @Override
  @Transactional
  public BulkGenerateResponse bulkGenerate(String trainNumber) {
    TrainEntity train = findTrain(trainNumber);
    validateTrainReady(train);

    TrainScheduleEntity schedule = findActiveSchedule(train);
    Set<RunDay> runDays = schedule.getRunDaysAsSet();

    LocalDate from  = LocalDate.now().plusDays(1);
    LocalDate to    = LocalDate.now().plusDays(BOOKING_WINDOW_DAYS);

    List<LocalDate> createdDates = new ArrayList<>();
    int skipped = 0;

    LocalDate cursor = from;
    while (!cursor.isAfter(to)) {
      String dayName = cursor.getDayOfWeek().name().substring(0, 3);
      boolean isScheduledDay = runDays.contains(RunDay.valueOf(dayName));

      if (isScheduledDay &&
        !journeyRepository.existsByTrain_TrainIdAndJourneyDate(train.getTrainId(), cursor)) {

        JourneyEntity journey = JourneyEntity.builder()
          .train(train)
          .schedule(schedule)
          .journeyDate(cursor)
          .isCancelled(false)
          .chartPrepared(false)
          .build();
        journeyRepository.save(journey);
        createdDates.add(cursor);
      } else {
        skipped++;
      }
      cursor = cursor.plusDays(1);
    }

    log.info("Bulk generate for train {} — created={} skipped={}",
      trainNumber, createdDates.size(), skipped);

    return BulkGenerateResponse.builder()
      .created(createdDates.size())
      .skipped(skipped)
      .total(BOOKING_WINDOW_DAYS)
      .from(from)
      .to(to)
      .createdDates(createdDates)
      .build();
  }

  // ── Generate single — exactly 120 days from today ─────────────────────────

  @Override
  @Transactional
  public JourneyResponse generateForTrain(String trainNumber) {
    TrainEntity train = findTrain(trainNumber);
    LocalDate targetDate = LocalDate.now().plusDays(BOOKING_WINDOW_DAYS);
    return createJourneyForDate(train, targetDate, "generate");
  }

  // ── Manual add ────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public JourneyResponse addJourney(String trainNumber, AddJourneyRequest request) {
    TrainEntity train = findTrain(trainNumber);
    return createJourneyForDate(train, request.getJourneyDate(), "manual");
  }

  // ── List journeys ─────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<JourneyResponse> getJourneysForTrain(String trainNumber) {
    TrainEntity train = findTrain(trainNumber);

    LocalDate from = LocalDate.now().minusDays(7);
    LocalDate to   = LocalDate.now().plusDays(BOOKING_WINDOW_DAYS);

    LocalTime sourceDeparture = trainStopRepository
      .findSourceStop(train.getTrainId())
      .map(TrainStopEntity::getDepartureTime)
      .orElse(null);

    return journeyRepository
      .findByTrainAndDateRange(train.getTrainId(), from, to)
      .stream()
      .map(j -> toResponse(j, sourceDeparture))
      .collect(Collectors.toList());
  }

  // ── Cancel journey ────────────────────────────────────────────────────────

  @Override
  @Transactional
  public void cancelJourney(String trainNumber, Long journeyId, CancelJourneyRequest request) {
    TrainEntity train = findTrain(trainNumber);

    JourneyEntity journey = journeyRepository.findById(journeyId)
      .orElseThrow(() -> new BaseException(
        HttpStatus.NOT_FOUND, "JOURNEY_NOT_FOUND",
        "Journey not found: " + journeyId));

    if (!journey.getTrain().getTrainId().equals(train.getTrainId())) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST, "JOURNEY_TRAIN_MISMATCH",
        "Journey does not belong to train " + trainNumber);
    }
    if (Boolean.TRUE.equals(journey.getIsCancelled())) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST, "JOURNEY_ALREADY_CANCELLED",
        "Journey is already cancelled");
    }
    if (Boolean.TRUE.equals(journey.getChartPrepared())) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST, "CHART_ALREADY_PREPARED",
        "Cannot cancel — chart is already prepared for this journey");
    }

    LocalTime srcDep = trainStopRepository
      .findSourceStop(train.getTrainId())
      .map(TrainStopEntity::getDepartureTime)
      .orElse(null);

    JourneyStatus status = journey.deriveStatus(srcDep);
    if (status == JourneyStatus.DEPARTED) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST, "JOURNEY_ALREADY_DEPARTED",
        "Cannot cancel a journey that has already departed");
    }
    if (status == JourneyStatus.COMPLETED) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST, "JOURNEY_ALREADY_COMPLETED",
        "Cannot cancel a journey that is already completed");
    }

    journeyRepository.cancelJourney(journeyId, request.getReason());
    log.info("Journey {} cancelled for train {}. Reason: {}",
      journeyId, trainNumber, request.getReason());
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private JourneyResponse createJourneyForDate(
    TrainEntity train, LocalDate date, String source) {

    validateTrainReady(train);

    if (journeyRepository.existsByTrain_TrainIdAndJourneyDate(train.getTrainId(), date)) {
      throw new BaseException(
        HttpStatus.CONFLICT, "JOURNEY_ALREADY_EXISTS",
        "Journey already exists for train " + train.getTrainNumber() + " on " + date);
    }

    TrainScheduleEntity schedule = findActiveSchedule(train);

    // Auto-generate enforces day-of-week. Manual add is admin override.
    if ("generate".equals(source) && !runsOnDate(schedule, date)) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST, "SCHEDULE_DAY_MISMATCH",
        "Train " + train.getTrainNumber() + " does not run on " +
          date.getDayOfWeek().name() + " per its schedule (" +
          schedule.getRunsOnDays() + ")");
    }

    JourneyEntity journey = JourneyEntity.builder()
      .train(train)
      .schedule(schedule)
      .journeyDate(date)
      .isCancelled(false)
      .chartPrepared(false)
      .build();

    JourneyEntity saved = journeyRepository.save(journey);
    log.info("Journey created [{}] for train {} on {}", source, train.getTrainNumber(), date);

    LocalTime srcDep = trainStopRepository
      .findSourceStop(train.getTrainId())
      .map(TrainStopEntity::getDepartureTime)
      .orElse(null);

    return toResponse(saved, srcDep);
  }

  private void validateTrainReady(TrainEntity train) {
    if (trainStopRepository.countByTrain_TrainId(train.getTrainId()) < 2) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST, "INSUFFICIENT_STOPS",
        "Train " + train.getTrainNumber() + " must have at least 2 stops configured");
    }
    if (trainCoachRepository.countByTrain_TrainIdAndIsActiveTrue(train.getTrainId()) == 0) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST, "NO_ACTIVE_COACHES",
        "Train " + train.getTrainNumber() + " must have at least one active coach configured");
    }
  }

  private TrainScheduleEntity findActiveSchedule(TrainEntity train) {
    return scheduleRepository
      .findRunning(train.getTrainId(), LocalDate.now())
      .orElseGet(() ->
        scheduleRepository.findUpcoming(train.getTrainId(), LocalDate.now())
          .stream().findFirst()
          .orElseThrow(() -> new BaseException(
            HttpStatus.BAD_REQUEST, "NO_ACTIVE_SCHEDULE",
            "No active schedule found for train " + train.getTrainNumber()))
      );
  }

  private TrainEntity findTrain(String trainNumber) {
    return trainRepository.findByTrainNumber(trainNumber)
      .orElseThrow(() -> new BaseException(
        HttpStatus.NOT_FOUND, "TRAIN_NOT_FOUND",
        "Train not found: " + trainNumber));
  }

  private boolean runsOnDate(TrainScheduleEntity schedule, LocalDate date) {
    if (date.isBefore(schedule.getStartDate())) return false;
    if (schedule.getEndDate() != null && date.isAfter(schedule.getEndDate())) return false;
    Set<RunDay> runDays = schedule.getRunDaysAsSet();
    String day = date.getDayOfWeek().name().substring(0, 3);
    try { return runDays.contains(RunDay.valueOf(day)); }
    catch (IllegalArgumentException e) { return false; }
  }

  private JourneyResponse toResponse(JourneyEntity j, LocalTime sourceDeparture) {
    return JourneyResponse.builder()
      .journeyId(j.getJourneyId())
      .journeyDate(j.getJourneyDate())
      .status(j.deriveStatus(sourceDeparture).name())
      .chartPrepared(Boolean.TRUE.equals(j.getChartPrepared()))
      .cancelled(Boolean.TRUE.equals(j.getIsCancelled()))
      .cancelReason(j.getCancelReason())
      .scheduleId(j.getSchedule().getScheduleId())
      .scheduleRunDays(j.getSchedule().getRunsOnDays())
      .createdAt(j.getCreatedAt() != null ? j.getCreatedAt().format(DT_FMT) : null)
      .build();
  }
}
