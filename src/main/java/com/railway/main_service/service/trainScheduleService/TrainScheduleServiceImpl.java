package com.railway.main_service.service.trainScheduleService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.trainSchedule.AddTrainScheduleRequest;
import com.railway.main_service.dto.response.trainSchedule.TrainScheduleResponse;
import com.railway.main_service.dto.response.trainSchedule.TrainScheduleSummaryResponse;
import com.railway.main_service.entity.TrainEntity;
import com.railway.main_service.entity.TrainScheduleEntity;
import com.railway.main_service.enums.RunDay;
import com.railway.main_service.repository.TrainRepository;
import com.railway.main_service.repository.TrainScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service @Loggable @Slf4j @RequiredArgsConstructor
public class TrainScheduleServiceImpl implements TrainScheduleService {

  private final TrainRepository         trainRepository;
  private final TrainScheduleRepository scheduleRepository;

  private static final int REMOVAL_BUFFER_DAYS  = 120;
  private static final int ADDITION_BUFFER_DAYS = 10;

  // ── Summary ───────────────────────────────────────────────────────────────
  @Override
  @Transactional(readOnly = true)
  public TrainScheduleSummaryResponse getSummary(String trainNumber) {
    TrainEntity train = findTrain(trainNumber);
    Long        tid   = train.getTrainId();
    LocalDate   today = LocalDate.now();

    Optional<TrainScheduleEntity> running     = scheduleRepository.findRunning(tid, today);
    List<TrainScheduleEntity>     upcoming    = scheduleRepository.findUpcoming(tid, today);
    List<TrainScheduleEntity>     past        = scheduleRepository.findPast(tid, today);
    List<TrainScheduleEntity>     deactivated = scheduleRepository.findDeactivated(tid);

    return TrainScheduleSummaryResponse.builder()
      .running(running.map(s -> toResponse(s, "RUNNING", null, null, trainNumber, null)).orElse(null))
      .upcoming(upcoming.stream()
        .map(s -> toResponse(s, "UPCOMING", null, null, trainNumber, null))
        .collect(Collectors.toList()))
      .past(past.stream()
        .map(s -> toResponse(s, "PAST", null, null, trainNumber, null))
        .collect(Collectors.toList()))
      .deactivated(deactivated.stream()
        .map(s -> toResponse(s, "DEACTIVATED", null, null, trainNumber, null))
        .collect(Collectors.toList()))
      .build();
  }

  // ── Create Schedule ───────────────────────────────────────────────────────
  @Override
  @Transactional
  public TrainScheduleResponse createSchedule(String trainNumber,
                                              AddTrainScheduleRequest req) {
    TrainEntity train     = findTrain(trainNumber);
    Long        tid       = train.getTrainId();
    Set<RunDay> newDays   = req.getRunDays();
    LocalDate   startDate = req.getStartDate();
    LocalDate   today     = LocalDate.now();

    // ── 1. Start date must be future ─────────────────────────────────────────
    if (!startDate.isAfter(today)) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_START_DATE",
        "Start date must be in the future (from tomorrow onwards).");
    }

    // ── 2. Block if any active upcoming already exists ───────────────────────
    if (scheduleRepository.hasActiveUpcoming(tid, today)) {
      throw new BaseException(HttpStatus.CONFLICT, "UPCOMING_SCHEDULE_EXISTS",
        "An upcoming schedule is already active. " +
          "Deactivate it before creating a new one.");
    }

    List<String> addedDays   = new ArrayList<>();
    List<String> removedDays = new ArrayList<>();

    // ── 3. Find running schedule for day-diff validation ─────────────────────
    Optional<TrainScheduleEntity> runningOpt = scheduleRepository.findRunning(tid, today);

    if (runningOpt.isPresent()) {
      TrainScheduleEntity running     = runningOpt.get();
      Set<RunDay>         currentDays = running.getRunDaysAsSet();

      // ── 4. Same days as running — no point ──────────────────────────────
      if (newDays.equals(currentDays)) {
        throw new BaseException(HttpStatus.BAD_REQUEST, "SCHEDULE_NO_CHANGE",
          "New schedule has the same days as the currently running schedule.");
      }

      // ── 5. Compute diff ──────────────────────────────────────────────────
      Set<RunDay> removed = EnumSet.copyOf(currentDays);
      removed.removeAll(newDays);

      Set<RunDay> added = EnumSet.copyOf(newDays);
      added.removeAll(currentDays);

      removedDays = toSortedNames(removed);
      addedDays   = toSortedNames(added);

      // ── 6. Buffer rules based on diff ────────────────────────────────────
      if (!removed.isEmpty()) {
        // Removals → 120-day buffer
        LocalDate minDate = today.plusDays(REMOVAL_BUFFER_DAYS);
        if (startDate.isBefore(minDate)) {
          throw new BaseException(HttpStatus.BAD_REQUEST, "START_DATE_TOO_SOON",
            "Removing days " + removedDays + " requires start date on or after " +
              minDate + " (" + REMOVAL_BUFFER_DAYS + " days from today). " +
              "Passengers may have already booked on those days.");
        }
      } else {
        // Only additions → 10-day buffer
        LocalDate minDate = today.plusDays(ADDITION_BUFFER_DAYS);
        if (startDate.isBefore(minDate)) {
          throw new BaseException(HttpStatus.BAD_REQUEST, "START_DATE_TOO_SOON",
            "Adding new days requires start date on or after " +
              minDate + " (" + ADDITION_BUFFER_DAYS + " days from today).");
        }
      }

      // ── 7. Set endDate on running schedule (DO NOT touch isActive) ───────
      running.setEndDate(startDate.minusDays(1));
      running.setUpdatedBy(SecurityUtils.getCurrentAdminId());
      scheduleRepository.save(running);

      log.info("Set endDate={} on running schedule {} for train {}",
        running.getEndDate(), running.getScheduleId(), trainNumber);

    } else {
      // ── No running schedule — only tomorrow minimum ───────────────────────
      LocalDate minDate = today.plusDays(1);
      if (startDate.isBefore(minDate)) {
        throw new BaseException(HttpStatus.BAD_REQUEST, "START_DATE_TOO_SOON",
          "Start date must be at least tomorrow (" + minDate + ").");
      }
      addedDays = toSortedNames(newDays);
    }

    // ── 8. Create new schedule ────────────────────────────────────────────────
    TrainScheduleEntity newSchedule = TrainScheduleEntity.builder()
      .train(train)
      .runsOnDays(TrainScheduleEntity.toDayString(newDays))
      .startDate(startDate)
      .endDate(null)   // indefinite until another schedule is created
      .isActive(true)
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    TrainScheduleEntity saved = scheduleRepository.save(newSchedule);

    log.info("Created schedule {} for train {} starting {}. Days: {}",
      saved.getScheduleId(), trainNumber, startDate, saved.getRunsOnDays());

    return toResponse(saved, "UPCOMING", addedDays, removedDays, trainNumber,
      "Schedule created successfully. Effective from " + startDate + ".");
  }

  // ── Toggle Schedule ───────────────────────────────────────────────────────
  @Override
  @Transactional
  public TrainScheduleResponse toggleSchedule(String trainNumber, Long scheduleId) {
    TrainEntity train = findTrain(trainNumber);
    LocalDate   today = LocalDate.now();

    TrainScheduleEntity schedule = scheduleRepository
      .findByScheduleIdAndTrain_TrainId(scheduleId, train.getTrainId())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND",
        "Schedule not found for train " + trainNumber + "."));

    // ── Determine current status ──────────────────────────────────────────────
    boolean isRunning = schedule.getIsActive()
      && !schedule.getStartDate().isAfter(today)
      && (schedule.getEndDate() == null || !schedule.getEndDate().isBefore(today));

    if (isRunning) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "CANNOT_TOGGLE_RUNNING",
        "Cannot deactivate a currently running schedule. " +
          "Create a new schedule to replace it — it will automatically end this one.");
    }

    boolean currentlyActive = schedule.getIsActive();

    if (!currentlyActive) {
      // ── Reactivating — check no other active upcoming exists ──────────────
      boolean isUpcoming = schedule.getStartDate().isAfter(today);
      if (isUpcoming && scheduleRepository.hasActiveUpcoming(train.getTrainId(), today)) {
        throw new BaseException(HttpStatus.CONFLICT, "UPCOMING_ALREADY_ACTIVE",
          "Another upcoming schedule is already active. " +
            "Deactivate it before reactivating this one.");
      }
    }

    // ── Toggle ────────────────────────────────────────────────────────────────
    schedule.setIsActive(!currentlyActive);
    schedule.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    scheduleRepository.save(schedule);

    String newStatus = deriveStatus(schedule, today);
    String msg = currentlyActive
      ? "Schedule deactivated."
      : "Schedule reactivated.";

    log.info("Toggled schedule {} for train {} → isActive={}",
      scheduleId, trainNumber, schedule.getIsActive());

    return toResponse(schedule, newStatus, null, null, trainNumber, msg);
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private TrainEntity findTrain(String trainNumber) {
    return trainRepository.findByTrainNumber(trainNumber.trim())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_NOT_FOUND",
        "Train not found: " + trainNumber));
  }

  private List<String> toSortedNames(Set<RunDay> days) {
    if (days == null || days.isEmpty()) return List.of();
    return days.stream()
      .sorted(Comparator.comparingInt(Enum::ordinal))
      .map(Enum::name)
      .collect(Collectors.toList());
  }

  private String deriveStatus(TrainScheduleEntity e, LocalDate today) {
    if (!e.getIsActive()) return "DEACTIVATED";
    if (e.getStartDate().isAfter(today)) return "UPCOMING";
    if (e.getEndDate() != null && e.getEndDate().isBefore(today)) return "PAST";
    return "RUNNING";
  }

  private TrainScheduleResponse toResponse(TrainScheduleEntity e,
                                           String status,
                                           List<String> addedDays,
                                           List<String> removedDays,
                                           String trainNumber,
                                           String message) {
    List<String> days = e.getRunDaysAsSet().stream()
      .sorted(Comparator.comparingInt(Enum::ordinal))
      .map(Enum::name)
      .collect(Collectors.toList());

    return TrainScheduleResponse.builder()
      .scheduleId(e.getScheduleId())
      .trainNumber(trainNumber)
      .runDays(days)
      .startDate(e.getStartDate())
      .endDate(e.getEndDate())
      .isActive(e.getIsActive())
      .status(status)
      .addedDays(addedDays)
      .removedDays(removedDays)
      .createdBy(e.getCreatedBy())
      .createdAt(e.getCreatedAt())
      .updatedAt(e.getUpdatedAt())
      .message(message)
      .build();
  }
}
