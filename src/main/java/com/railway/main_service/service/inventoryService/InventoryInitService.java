package com.railway.main_service.service.inventoryService;

import com.railway.main_service.entity.*;
import com.railway.main_service.enums.QuotaType;
import com.railway.main_service.repository.JourneyRepository;
import com.railway.main_service.repository.JourneySeatInventoryRepository;
import com.railway.main_service.repository.TrainCoachRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryInitService {

  private final JourneySeatInventoryRepository inventoryRepository;
  private final TrainCoachRepository           trainCoachRepository;
  private final JourneyRepository              journeyRepository;

  // ── Called when a new journey is created ─────────────────────────────────
  // Finds coaches active on the journey date and creates inventory rows
  @Transactional
  public void initForJourney(JourneyEntity journey) {
    Long      trainId     = journey.getTrain().getTrainId();
    LocalDate journeyDate = journey.getJourneyDate();

    if (inventoryRepository.existsByJourney_JourneyId(journey.getJourneyId())) {
      log.warn("Inventory already exists for journey {} — skipping", journey.getJourneyId());
      return;
    }

    // Use date-aware query instead of isActive flag
    List<TrainCoachEntity> coaches =
      trainCoachRepository.findActiveCoachesForDate(trainId, journeyDate);

    if (coaches.isEmpty()) {
      log.warn("No active coaches for train {} on {} — inventory not created for journey {}",
        trainId, journeyDate, journey.getJourneyId());
      return;
    }

    inventoryRepository.saveAll(buildRows(journey, coaches));
    log.info("Inventory initialised for journey {} on {} — {} rows",
      journey.getJourneyId(), journeyDate, coaches.size() * 2);
  }

  // ── Called when a new coach type is added to a train ─────────────────────
  // Finds all future journeys that fall within the coach's effective range
  // and creates inventory rows for each — skips any that already have one
  @Transactional
  public void initForNewCoach(TrainCoachEntity coach) {
    Long      trainId       = coach.getTrain().getTrainId();
    LocalDate effectiveFrom = coach.getEffectiveFrom();
    LocalDate effectiveTill = coach.getEffectiveTill(); // null = open-ended

    // Find all non-cancelled future journeys for this train in the effective range
    List<JourneyEntity> journeys = effectiveTill != null
      ? journeyRepository.findByTrainIdAndDateRange(trainId, effectiveFrom, effectiveTill)
      : journeyRepository.findByTrainIdFromDate(trainId, effectiveFrom);

    if (journeys.isEmpty()) {
      log.info("No journeys found for new coach {} from {} — no inventory created",
        coach.getCoachId(), effectiveFrom);
      return;
    }

    List<JourneySeatInventoryEntity> rows = new ArrayList<>();
    int skipped = 0;

    for (JourneyEntity journey : journeys) {
      // Skip if this coach type already has inventory for this journey
      boolean exists = inventoryRepository
        .existsByJourney_JourneyIdAndTrainCoach_CoachId(
          journey.getJourneyId(), coach.getCoachId());
      if (exists) { skipped++; continue; }

      rows.addAll(buildRows(journey, List.of(coach)));
    }

    if (!rows.isEmpty()) inventoryRepository.saveAll(rows);
    log.info("Coach {} inventory init: {} rows created, {} journeys skipped",
      coach.getCoachId(), rows.size(), skipped);
  }

  // ── Shared row builder ────────────────────────────────────────────────────
  private List<JourneySeatInventoryEntity> buildRows(
    JourneyEntity journey, List<TrainCoachEntity> coaches) {

    List<JourneySeatInventoryEntity> rows = new ArrayList<>();

    for (TrainCoachEntity coach : coaches) {
      int seatsPerCoach = coach.getCoachType().getTotalSeats();
      int coachCount    = coach.getCoachCount();

      // GENERAL row
      rows.add(JourneySeatInventoryEntity.builder()
        .journey(journey)
        .trainCoach(coach)
        .quotaType(QuotaType.GENERAL)
        .totalSeats(seatsPerCoach * coachCount)
        .bookedConfirmed(0)
        .totalRac(coach.getRacSeats() * coachCount)
        .bookedRac(0)
        .waitlistLimit(coach.getWaitlistLimit())
        .bookedWaitlist(0)
        .build());

      // TATKAL row — only if tatkal seats are configured
      if (coach.getTatkalSeats() != null && coach.getTatkalSeats() > 0) {
        rows.add(JourneySeatInventoryEntity.builder()
          .journey(journey)
          .trainCoach(coach)
          .quotaType(QuotaType.TATKAL)
          .totalSeats(coach.getTatkalSeats() * coachCount)
          .bookedConfirmed(0)
          .totalRac(null)
          .bookedRac(0)
          .waitlistLimit(null)
          .bookedWaitlist(0)
          .build());
      }
    }
    return rows;
  }
}
