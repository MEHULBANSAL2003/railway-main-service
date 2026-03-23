package com.railway.main_service.service.inventoryService;

import com.railway.main_service.entity.*;
import com.railway.main_service.enums.QuotaType;
import com.railway.main_service.repository.JourneySeatInventoryRepository;
import com.railway.main_service.repository.TrainCoachRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryInitService {

  private final JourneySeatInventoryRepository inventoryRepository;
  private final TrainCoachRepository           trainCoachRepository;

  /**
   * Creates inventory rows for a newly created journey.
   * Called from JourneyServiceImpl right after journey is saved.
   *
   * For each active TrainCoach on the train:
   *   → 1 GENERAL row  (confirmed + RAC + waitlist)
   *   → 1 TATKAL row   (confirmed only, RAC/WL are null)
   */
  @Transactional
  public void initForJourney(JourneyEntity journey) {
    Long trainId = journey.getTrain().getTrainId();

    // Guard — skip if already initialised (e.g. manual re-trigger)
    if (inventoryRepository.existsByJourney_JourneyId(journey.getJourneyId())) {
      log.warn("Inventory already exists for journey {} — skipping init", journey.getJourneyId());
      return;
    }

    List<TrainCoachEntity> coaches =
      trainCoachRepository.findActiveByTrainIdOnDate(trainId, journey.getJourneyDate());

    if (coaches.isEmpty()) {
      log.warn("No active coaches for train {} — inventory not created for journey {}",
        trainId, journey.getJourneyId());
      return;
    }

    List<JourneySeatInventoryEntity> rows = new ArrayList<>();

    for (TrainCoachEntity coach : coaches) {
      int seatsPerCoach = coach.getCoachType().getTotalSeats();
      int coachCount    = coach.getCoachCount();

      // ── GENERAL row ──────────────────────────────────────────────────────
      rows.add(JourneySeatInventoryEntity.builder()
        .journey(journey)
        .trainCoach(coach)
        .quotaType(QuotaType.GENERAL)
        // confirmed = seats per coach × number of coaches
        .totalSeats(seatsPerCoach * coachCount)
        .bookedConfirmed(0)
        // RAC = rac slots per coach × number of coaches
        .totalRac(coach.getRacSeats() * coachCount)
        .bookedRac(0)
        // waitlist = flat limit (not multiplied)
        .waitlistLimit(coach.getWaitlistLimit())
        .bookedWaitlist(0)
        .build());

      // ── TATKAL row ───────────────────────────────────────────────────────
      // Only create if train has tatkal seats configured for this coach
      if (coach.getTatkalSeats() != null && coach.getTatkalSeats() > 0) {
        rows.add(JourneySeatInventoryEntity.builder()
          .journey(journey)
          .trainCoach(coach)
          .quotaType(QuotaType.TATKAL)
          // tatkal confirmed = tatkal seats per coach × number of coaches
          .totalSeats(coach.getTatkalSeats() * coachCount)
          .bookedConfirmed(0)
          // RAC and WL are null for TATKAL
          .totalRac(null)
          .bookedRac(0)
          .waitlistLimit(null)
          .bookedWaitlist(0)
          .build());
      }
    }

    inventoryRepository.saveAll(rows);
    log.info("Inventory initialised for journey {} — {} rows created",
      journey.getJourneyId(), rows.size());
  }
}
