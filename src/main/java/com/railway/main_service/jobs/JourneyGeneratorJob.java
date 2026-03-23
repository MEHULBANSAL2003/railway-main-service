package com.railway.main_service.jobs;

import com.railway.main_service.entity.*;
import com.railway.main_service.enums.RunDay;
import com.railway.main_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Runs every night at 00:05.
 * For each active schedule, checks if a journey exists 120 days ahead.
 * If not — creates one.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JourneyGeneratorJob {

  private static final int BOOKING_WINDOW_DAYS = 120;

  private final TrainScheduleRepository scheduleRepository;
  private final JourneyRepository       journeyRepository;
  private final TrainStopRepository     trainStopRepository;
  private final TrainCoachRepository    trainCoachRepository;

  @Scheduled(cron = "0 5 0 * * *")
  public void run() {
    LocalDate targetDate = LocalDate.now().plusDays(BOOKING_WINDOW_DAYS);
    log.info("JourneyGeneratorJob — target date: {}", targetDate);

    List<TrainScheduleEntity> schedules =
      scheduleRepository.findActiveSchedulesForDate(targetDate);

    int created = 0, skipped = 0;

    for (TrainScheduleEntity schedule : schedules) {
      try {
        boolean ran = process(schedule, targetDate);
        if (ran) created++; else skipped++;
      } catch (Exception e) {
        log.error("JourneyGeneratorJob — failed for schedule {} train {}: {}",
          schedule.getScheduleId(),
          schedule.getTrain().getTrainNumber(),
          e.getMessage());
      }
    }

    log.info("JourneyGeneratorJob done — created={} skipped={}", created, skipped);
  }

  @Transactional
  public boolean process(TrainScheduleEntity schedule, LocalDate targetDate) {
    if (!runsOnDate(schedule, targetDate)) return false;

    TrainEntity train = schedule.getTrain();

    if (journeyRepository.existsByTrain_TrainIdAndJourneyDate(
      train.getTrainId(), targetDate)) return false;

    // Skip if train not ready
    if (trainStopRepository.countByTrain_TrainId(train.getTrainId()) < 2) return false;
    if (trainCoachRepository.countActiveByTrainIdOnDate(train.getTrainId(), targetDate) == 0) return false;

    JourneyEntity journey = JourneyEntity.builder()
      .train(train)
      .schedule(schedule)
      .journeyDate(targetDate)
      .isCancelled(false)
      .chartPrepared(false)
      .build();

    journeyRepository.save(journey);
    log.debug("Journey created — train {} date {}", train.getTrainNumber(), targetDate);
    return true;
  }

  private boolean runsOnDate(TrainScheduleEntity schedule, LocalDate date) {
    if (date.isBefore(schedule.getStartDate())) return false;
    if (schedule.getEndDate() != null && date.isAfter(schedule.getEndDate())) return false;

    Set<RunDay> runDays = schedule.getRunDaysAsSet();
    String day = date.getDayOfWeek().name().substring(0, 3);
    try {
      return runDays.contains(RunDay.valueOf(day));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
