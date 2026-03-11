package com.railway.main_service.service.trainCoachService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.trainCoach.ChangeCoachConfigRequest;
import com.railway.main_service.dto.request.trainCoach.DeactivateCoachRequest;
import com.railway.main_service.dto.response.trainCoach.CoachConfigChangeResponse;
import com.railway.main_service.dto.response.trainCoach.CoachConfigConflictItem;
import com.railway.main_service.entity.JourneySeatInventoryEntity;
import com.railway.main_service.entity.TrainCoachEntity;
import com.railway.main_service.entity.TrainEntity;
import com.railway.main_service.enums.QuotaType;
import com.railway.main_service.repository.JourneySeatInventoryRepository;
import com.railway.main_service.repository.TrainCoachRepository;
import com.railway.main_service.repository.TrainRepository;
import com.railway.main_service.service.inventoryService.InventoryInitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrainCoachConfigService {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final TrainCoachRepository           trainCoachRepository;
  private final TrainRepository                trainRepository;
  private final JourneySeatInventoryRepository inventoryRepository;

  // ── Change Config ─────────────────────────────────────────────────────────
  @Transactional
  public CoachConfigChangeResponse changeConfig(String trainNumber, Long coachId,
                                                ChangeCoachConfigRequest req) {
    TrainEntity      train = findTrain(trainNumber);
    TrainCoachEntity coach = findCoach(coachId, train.getTrainId());

    validateDateRange(req.getEffectiveFrom(), req.getEffectiveTo());

    int seatsPerCoach = coach.getCoachType().getTotalSeats();

    // Validate per-coach quotas (tatkal + rac <= seatsPerCoach)
    validatePerCoachQuotas(req.getTatkalSeats(), req.getRacSeats(),
      seatsPerCoach, coach.getCoachType().getTypeCode());

    // Compute new totals
    int newTotalSeats    = req.getCoachCount() * seatsPerCoach;
    int newTotalTatkal   = req.getCoachCount() * req.getTatkalSeats();
    int newTotalRac      = req.getCoachCount() * req.getRacSeats();
    int newWaitlistLimit = req.getWaitlistLimit();

    // ── Conflict check — only needed if decreasing any value ─────────────────
    List<CoachConfigConflictItem> conflicts = checkConflicts(
      coach, req.getEffectiveFrom(), req.getEffectiveTo(),
      newTotalSeats, newTotalTatkal, newTotalRac, newWaitlistLimit
    );

    if (!conflicts.isEmpty()) {
      return CoachConfigChangeResponse.builder()
        .success(false)
        .message("Config change blocked — " + conflicts.size() +
          " journey(s) have bookings that exceed the new limits.")
        .conflicts(conflicts)
        .build();
    }

    // ── Apply: close current row, insert new ─────────────────────────────────
    coach.setEffectiveTo(req.getEffectiveFrom().minusDays(1));
    coach.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    trainCoachRepository.saveAndFlush(coach);

    TrainCoachEntity newConfig = TrainCoachEntity.builder()
      .train(coach.getTrain())
      .coachType(coach.getCoachType())
      .coachCount(req.getCoachCount())
      .tatkalSeats(req.getTatkalSeats())
      .racSeats(req.getRacSeats())
      .waitlistLimit(req.getWaitlistLimit())
      .isActive(true)
      .effectiveFrom(req.getEffectiveFrom())
      .effectiveTo(req.getEffectiveTo())
      .changeReason(req.getChangeReason())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    TrainCoachEntity saved = trainCoachRepository.save(newConfig);

    // ── Update inventory rows in date range ──────────────────────────────────
    int generalUpdated = inventoryRepository.updateGeneralInventory(
      saved.getCoachId(), newTotalSeats, newTotalRac, newWaitlistLimit,
      req.getEffectiveFrom(), req.getEffectiveTo()
    );

    int tatkalUpdated = inventoryRepository.updateTatkalInventory(
      saved.getCoachId(), newTotalTatkal,
      req.getEffectiveFrom(), req.getEffectiveTo()
    );

    // Also update old coachId's inventory rows to point to new coachId
    // (inventory rows still reference the old coachId — update them)
    int genOld = inventoryRepository.updateGeneralInventory(
      coachId, newTotalSeats, newTotalRac, newWaitlistLimit,
      req.getEffectiveFrom(), req.getEffectiveTo()
    );
    int tatOld = inventoryRepository.updateTatkalInventory(
      coachId, newTotalTatkal,
      req.getEffectiveFrom(), req.getEffectiveTo()
    );

    int affected = genOld + tatOld;

    log.info("Coach config changed for train {} coachId {} → newCoachId {} | {} inventory rows updated",
      trainNumber, coachId, saved.getCoachId(), affected);

    return CoachConfigChangeResponse.builder()
      .success(true)
      .message("Config updated from " + req.getEffectiveFrom().format(DATE_FMT) +
        (req.getEffectiveTo() != null ? " to " + req.getEffectiveTo().format(DATE_FMT) : " onwards") +
        ". " + affected + " journey inventory rows updated.")
      .affectedJourneys(affected)
      .build();
  }

  // ── Deactivate ────────────────────────────────────────────────────────────
  @Transactional
  public CoachConfigChangeResponse deactivate(String trainNumber, Long coachId,
                                              DeactivateCoachRequest req) {
    TrainEntity      train = findTrain(trainNumber);
    TrainCoachEntity coach = findCoach(coachId, train.getTrainId());

    if (!Boolean.TRUE.equals(coach.getIsActive()))
      throw new BaseException(HttpStatus.BAD_REQUEST, "COACH_ALREADY_INACTIVE",
        "Coach is already inactive.");

    validateDateRange(req.getEffectiveFrom(), req.getEffectiveTo());

    // Check if any journey in the date range has bookings
    List<JourneySeatInventoryEntity> inventoryRows =
      inventoryRepository.findByCoachFromDate(coachId, req.getEffectiveFrom());

    // Filter to date range if effectiveTo is set
    if (req.getEffectiveTo() != null) {
      inventoryRows = inventoryRows.stream()
        .filter(i -> !i.getJourney().getJourneyDate().isAfter(req.getEffectiveTo()))
        .toList();
    }

    List<CoachConfigConflictItem> conflicts = new ArrayList<>();
    for (JourneySeatInventoryEntity row : inventoryRows) {
      boolean hasBookings = row.getBookedConfirmed() > 0 ||
        row.getBookedRac()        > 0 ||
        row.getBookedWaitlist()   > 0;
      if (hasBookings) {
        conflicts.add(CoachConfigConflictItem.builder()
          .journeyId(row.getJourney().getJourneyId())
          .journeyDate(row.getJourney().getJourneyDate().format(DATE_FMT))
          .conflictField("hasBookings")
          .currentBooked(row.getBookedConfirmed() + row.getBookedRac() + row.getBookedWaitlist())
          .newLimit(0)
          .build());
      }
    }

    if (!conflicts.isEmpty()) {
      return CoachConfigChangeResponse.builder()
        .success(false)
        .message("Cannot deactivate — " + conflicts.size() +
          " journey(s) have existing bookings for this coach class.")
        .conflicts(conflicts)
        .build();
    }

    // Close the row
    coach.setEffectiveTo(req.getEffectiveFrom().minusDays(1));
    coach.setIsActive(false);
    coach.setChangeReason(req.getChangeReason());
    coach.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    trainCoachRepository.save(coach);

    // Delete unbooked inventory rows in range
    int deleted = inventoryRepository.deleteUnbookedInventory(
      coachId, req.getEffectiveFrom(), req.getEffectiveTo()
    );

    log.info("Coach {} deactivated for train {} from {} | {} inventory rows deleted",
      coachId, trainNumber, req.getEffectiveFrom(), deleted);

    return CoachConfigChangeResponse.builder()
      .success(true)
      .message("Coach deactivated from " + req.getEffectiveFrom().format(DATE_FMT) +
        (req.getEffectiveTo() != null ? " to " + req.getEffectiveTo().format(DATE_FMT) : " onwards") +
        ". " + deleted + " future inventory rows removed.")
      .affectedJourneys(deleted)
      .build();
  }

  // ── Conflict checker ──────────────────────────────────────────────────────
  private List<CoachConfigConflictItem> checkConflicts(
    TrainCoachEntity coach,
    LocalDate fromDate, LocalDate toDate,
    int newTotalSeats, int newTotalTatkal, int newTotalRac, int newWaitlist) {

    List<JourneySeatInventoryEntity> rows =
      inventoryRepository.findByCoachFromDate(coach.getCoachId(), fromDate);

    if (toDate != null)
      rows = rows.stream()
        .filter(i -> !i.getJourney().getJourneyDate().isAfter(toDate))
        .toList();

    List<CoachConfigConflictItem> conflicts = new ArrayList<>();

    for (JourneySeatInventoryEntity row : rows) {
      String date = row.getJourney().getJourneyDate().format(DATE_FMT);
      Long   jid  = row.getJourney().getJourneyId();

      if (row.getQuotaType() == QuotaType.GENERAL) {
        if (row.getBookedConfirmed() > newTotalSeats)
          conflicts.add(conflict(jid, date, "confirmedSeats", row.getBookedConfirmed(), newTotalSeats));

        if (row.getTotalRac() != null && row.getBookedRac() > newTotalRac)
          conflicts.add(conflict(jid, date, "racSeats", row.getBookedRac(), newTotalRac));

        if (row.getWaitlistLimit() != null && row.getBookedWaitlist() > newWaitlist)
          conflicts.add(conflict(jid, date, "waitlistLimit", row.getBookedWaitlist(), newWaitlist));

      } else if (row.getQuotaType() == QuotaType.TATKAL) {
        if (row.getBookedConfirmed() > newTotalTatkal)
          conflicts.add(conflict(jid, date, "tatkalSeats", row.getBookedConfirmed(), newTotalTatkal));
      }
    }

    return conflicts;
  }

  private CoachConfigConflictItem conflict(Long jid, String date,
                                           String field, int booked, int newLimit) {
    return CoachConfigConflictItem.builder()
      .journeyId(jid).journeyDate(date)
      .conflictField(field).currentBooked(booked).newLimit(newLimit)
      .build();
  }

  private void validateDateRange(LocalDate from, LocalDate to) {
    if (to != null && !to.isAfter(from))
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
        "Effective-to date must be after effective-from date.");
  }

  private void validatePerCoachQuotas(int tatkal, int rac, int total, String typeCode) {
    if (tatkal > total)
      throw new BaseException(HttpStatus.BAD_REQUEST, "TATKAL_EXCEEDS_TOTAL",
        "Tatkal (" + tatkal + ") cannot exceed seats per coach (" + total + ").");
    if (rac > total)
      throw new BaseException(HttpStatus.BAD_REQUEST, "RAC_EXCEEDS_TOTAL",
        "RAC (" + rac + ") cannot exceed seats per coach (" + total + ").");
    if ((tatkal + rac) > total)
      throw new BaseException(HttpStatus.BAD_REQUEST, "QUOTA_OVERLAP",
        "Tatkal (" + tatkal + ") + RAC (" + rac + ") exceeds seats per coach (" + total + ").");
  }

  private TrainEntity findTrain(String trainNumber) {
    return trainRepository.findByTrainNumber(trainNumber.trim())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_NOT_FOUND",
        "Train not found: " + trainNumber));
  }

  private TrainCoachEntity findCoach(Long coachId, Long trainId) {
    return trainCoachRepository.findByCoachIdAndTrain_TrainId(coachId, trainId)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "COACH_NOT_FOUND",
        "Coach not found on this train."));
  }
}
