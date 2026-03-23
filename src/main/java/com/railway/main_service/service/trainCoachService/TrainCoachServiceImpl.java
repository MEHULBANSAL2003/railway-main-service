package com.railway.main_service.service.trainCoachService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.DeactivateRequest;
import com.railway.main_service.dto.request.trainCoach.AddTrainCoachRequest;
import com.railway.main_service.dto.request.trainCoach.UpdateTrainCoachRequest;
import com.railway.main_service.dto.response.DeactivationResponse;
import com.railway.main_service.dto.response.trainCoach.CoachTypeDropdownResponse;
import com.railway.main_service.dto.response.trainCoach.TrainCoachResponse;
import com.railway.main_service.dto.response.trainCoach.TrainCopyCoachesResponse;
import com.railway.main_service.entity.CoachTypeEntity;
import com.railway.main_service.entity.JourneySeatInventoryEntity;
import com.railway.main_service.entity.TrainCoachEntity;
import com.railway.main_service.entity.TrainEntity;
import com.railway.main_service.enums.QuotaType;
import com.railway.main_service.repository.CoachTypeRepository;
import com.railway.main_service.repository.JourneySeatInventoryRepository;
import com.railway.main_service.repository.TrainCoachRepository;
import com.railway.main_service.repository.TrainPeriodRepository;
import com.railway.main_service.repository.TrainRepository;
import com.railway.main_service.repository.TrainTypeCoachRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service @Loggable @Slf4j @RequiredArgsConstructor
public class TrainCoachServiceImpl implements TrainCoachService {

  private final TrainCoachRepository           trainCoachRepository;
  private final TrainRepository                trainRepository;
  private final CoachTypeRepository            coachTypeRepository;
  private final TrainTypeCoachRepository       trainTypeCoachRepository;
  private final TrainPeriodRepository          trainPeriodRepository;
  private final JourneySeatInventoryRepository inventoryRepository;

  // ── Add ───────────────────────────────────────────────────────────────────
  @Override
  @Transactional
  public TrainCoachResponse addCoach(String trainNumber, AddTrainCoachRequest req) {

    TrainEntity     train     = findActiveTrain(trainNumber);
    CoachTypeEntity coachType = findActiveCoachType(req.getCoachTypeCode());

    // One row per train + coachType
    if (trainCoachRepository.existsByTrain_TrainIdAndCoachType_TypeId(
      train.getTrainId(), coachType.getTypeId()))
      throw new BaseException(HttpStatus.CONFLICT, "COACH_ALREADY_EXISTS",
        "Train " + trainNumber + " already has coach type '" +
          coachType.getTypeCode() + "'. Edit the existing row to change values.");

    int totalSeats = coachType.getTotalSeats();
    validatePerCoachQuotas(req.getTatkalSeats(), req.getRacSeats(),
      totalSeats, coachType.getTypeCode());

    TrainCoachEntity entity = TrainCoachEntity.builder()
      .train(train)
      .coachType(coachType)
      .coachCount(req.getCoachCount())
      .tatkalSeats(req.getTatkalSeats())
      .racSeats(req.getRacSeats())
      .waitlistLimit(req.getWaitlistLimit())
      .effectiveFrom(LocalDate.now())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    return toResponse(trainCoachRepository.save(entity), "Coach added successfully.");
  }

  // ── Update ────────────────────────────────────────────────────────────────
  @Override
  @Transactional
  public TrainCoachResponse updateCoach(String trainNumber, Long coachId,
                                        UpdateTrainCoachRequest req) {
    TrainEntity      train = findActiveTrain(trainNumber);
    TrainCoachEntity e     = findCoach(coachId, train.getTrainId());
    int totalSeats = e.getCoachType().getTotalSeats();

    // Calculate new totals before applying
    int newCoachCount = req.getCoachCount() != null ? req.getCoachCount() : e.getCoachCount();
    int newTatkal     = req.getTatkalSeats() != null ? req.getTatkalSeats() : e.getTatkalSeats();
    int newRac        = req.getRacSeats() != null ? req.getRacSeats() : e.getRacSeats();
    int newWaitlist   = req.getWaitlistLimit() != null ? req.getWaitlistLimit() : e.getWaitlistLimit();

    validatePerCoachQuotas(newTatkal, newRac, totalSeats, e.getCoachType().getTypeCode());

    int seatsPerCoach   = e.getCoachType().getTotalSeats();
    int newTotalSeats   = seatsPerCoach * newCoachCount;
    int newTotalRac     = newRac * newCoachCount;
    int newTotalTatkal  = newTatkal * newCoachCount;

    // ── Booking conflict validation ───────────────────────────────────────
    LocalDate today = LocalDate.now();
    List<JourneySeatInventoryEntity> futureInventory =
      inventoryRepository.findByCoachFromDate(e.getCoachId(), today);

    List<String> conflicts = new ArrayList<>();

    for (JourneySeatInventoryEntity inv : futureInventory) {
      if (inv.getQuotaType() == QuotaType.GENERAL) {
        if (inv.getBookedConfirmed() > newTotalSeats)
          conflicts.add("Journey " + inv.getJourney().getJourneyDate() + ": " +
            inv.getBookedConfirmed() + " confirmed > " + newTotalSeats + " new capacity");
        if (inv.getBookedRac() > newTotalRac)
          conflicts.add("Journey " + inv.getJourney().getJourneyDate() + ": " +
            inv.getBookedRac() + " RAC > " + newTotalRac + " new RAC capacity");
        if (inv.getBookedWaitlist() > newWaitlist)
          conflicts.add("Journey " + inv.getJourney().getJourneyDate() + ": " +
            inv.getBookedWaitlist() + " waitlist > " + newWaitlist + " new limit");
      }
      if (inv.getQuotaType() == QuotaType.TATKAL) {
        if (inv.getBookedConfirmed() > newTotalTatkal)
          conflicts.add("Journey " + inv.getJourney().getJourneyDate() + ": " +
            inv.getBookedConfirmed() + " tatkal booked > " + newTotalTatkal + " new tatkal capacity");
      }
    }

    if (!conflicts.isEmpty()) {
      throw new BaseException(HttpStatus.CONFLICT, "BOOKING_CONFLICT",
        "Cannot reduce capacity — existing bookings would be compromised:\n" +
          String.join("\n", conflicts));
    }

    // ── Safe to apply — update the entity ─────────────────────────────────
    e.setCoachCount(newCoachCount);
    e.setTatkalSeats(newTatkal);
    e.setRacSeats(newRac);
    e.setWaitlistLimit(newWaitlist);
    e.setUpdatedBy(SecurityUtils.getCurrentAdminId());

    // ── Update existing inventory to reflect new totals ───────────────────
    inventoryRepository.updateGeneralInventory(
      e.getCoachId(), newTotalSeats, newTotalRac, newWaitlist, today, e.getEffectiveTo());

    if (newTotalTatkal > 0) {
      inventoryRepository.updateTatkalInventory(
        e.getCoachId(), newTotalTatkal, today, e.getEffectiveTo());
    }

    return toResponse(trainCoachRepository.save(e), "Coach updated successfully.");
  }

  // ── Deactivate ────────────────────────────────────────────────────────────
  @Override
  @Transactional
  public DeactivationResponse deactivate(String trainNumber, Long coachId,
                                         DeactivateRequest request) {

    TrainEntity      train = findActiveTrain(trainNumber);
    TrainCoachEntity e     = findCoach(coachId, train.getTrainId());

    LocalDate fromDate = request.getFromDate();

    // Set effectiveTo: if fromDate is today, set effectiveTo to yesterday; otherwise fromDate - 1
    LocalDate effectiveTo = fromDate.minusDays(1);
    e.setEffectiveTo(effectiveTo);
    if (request.getReason() != null) {
      e.setChangeReason(request.getReason());
    }
    e.setUpdatedBy(SecurityUtils.getCurrentAdminId());

    // ── Check for booking conflicts from the deactivation date ────────────
    List<JourneySeatInventoryEntity> booked =
      inventoryRepository.findByCoachFromDate(coachId, fromDate);

    List<JourneySeatInventoryEntity> withBookings = booked.stream()
      .filter(i -> i.getBookedConfirmed() > 0 || i.getBookedRac() > 0 || i.getBookedWaitlist() > 0)
      .toList();

    if (!withBookings.isEmpty()) {
      List<String> conflictDates = withBookings.stream()
        .map(i -> "Journey " + i.getJourney().getJourneyDate() +
          " — confirmed: " + i.getBookedConfirmed() +
          ", RAC: " + i.getBookedRac() +
          ", waitlist: " + i.getBookedWaitlist())
        .distinct()
        .toList();

      throw new BaseException(HttpStatus.CONFLICT, "BOOKING_CONFLICT",
        "Cannot deactivate coach — existing bookings found on future journeys:\n" +
          String.join("\n", conflictDates));
    }

    // ── Delete unbooked inventory from deactivation date ──────────────────
    int deletedRows = inventoryRepository.deleteUnbookedInventory(coachId, fromDate, null);

    trainCoachRepository.save(e);

    log.info("Deactivated coach {} (type {}) on train {} from {}, deleted {} inventory rows",
      coachId, e.getCoachType().getTypeCode(), trainNumber, fromDate, deletedRows);

    return DeactivationResponse.builder()
      .entityType("TRAIN_COACH")
      .entityCode(e.getCoachType().getTypeCode())
      .entityName(e.getCoachType().getTypeName() + " on " + trainNumber)
      .action("DEACTIVATED")
      .deletedInventoryRows(deletedRows)
      .message("Coach type '" + e.getCoachType().getTypeCode() +
        "' deactivated on train " + trainNumber +
        " effective from " + fromDate + ".")
      .build();
  }

  // ── Get active coaches for a train ─────────────────────────────────────
  @Override
  public List<TrainCoachResponse> getAllByTrain(String trainNumber) {
    TrainEntity train = findTrainByNumber(trainNumber);
    LocalDate today = LocalDate.now();
    return trainCoachRepository.findActiveByTrainIdOnDate(train.getTrainId(), today)
      .stream().map(e -> toResponse(e, null)).toList();
  }

  // ── Get inactive coaches for a train ───────────────────────────────────
  @Override
  public List<TrainCoachResponse> getInactiveByTrain(String trainNumber) {
    TrainEntity train = findTrainByNumber(trainNumber);
    LocalDate today = LocalDate.now();
    return trainCoachRepository.findAllByTrainId(train.getTrainId()).stream()
      .filter(e -> !e.isActiveOn(today))
      .map(e -> toResponse(e, null))
      .toList();
  }

  // ── Get full coach history for a train ─────────────────────────────────
  @Override
  public List<TrainCoachResponse> getCoachHistory(String trainNumber) {
    TrainEntity train = findTrainByNumber(trainNumber);
    return trainCoachRepository.findAllByTrainId(train.getTrainId())
      .stream().map(e -> toResponse(e, null)).toList();
  }

  // ── Get all coaches including inactive ─────────────────────────────────
  @Override
  public List<TrainCoachResponse> getAllByTrainIncludingInactive(String trainNumber) {
    TrainEntity train = findTrainByNumber(trainNumber);
    return trainCoachRepository.findAllByTrainId(train.getTrainId())
      .stream().map(e -> toResponse(e, null)).toList();
  }

  // ── Available coach types dropdown ─────────────────────────────────────
  @Override
  public List<CoachTypeDropdownResponse> getAvailableCoachTypes(String trainNumber) {
    TrainEntity train = findTrainByNumber(trainNumber);
    LocalDate today = LocalDate.now();

    // Step 1 — which coach types are allowed for this train's type?
    List<Long> allowedIds = trainTypeCoachRepository
      .findAllowedCoachTypeIds(train.getTrainType().getTypeId());

    if (allowedIds.isEmpty()) {
      // No allowed coaches configured for this train type yet
      // Return empty — forces admin to configure train type first
      return List.of();
    }

    // Step 2 — which of those are already added to this train?
    List<Long> usedIds = trainCoachRepository
      .findUsedCoachTypeIdsByTrainId(train.getTrainId());

    // Step 3 — allowed AND active (period-based) AND not already added
    List<Long> availableIds = allowedIds.stream()
      .filter(id -> !usedIds.contains(id))
      .toList();

    if (availableIds.isEmpty()) return List.of(); // all allowed types already added

    return coachTypeRepository.findAllByTypeIdInAndActiveOnDate(availableIds, today).stream()
      .sorted(Comparator.comparing(CoachTypeEntity::getTypeCode))
      .map(ct -> CoachTypeDropdownResponse.builder()
        .typeId(ct.getTypeId())
        .typeCode(ct.getTypeCode())
        .typeName(ct.getTypeName())
        .totalSeats(ct.getTotalSeats())
        .isAc(ct.getIsAc())
        .build())
      .toList();
  }

  // ── Copy coaches ───────────────────────────────────────────────────────
  @Override
  @Transactional
  public TrainCopyCoachesResponse copyCoaches(String sourceTrainNumber,
                                              String targetTrainNumber) {

    // ── 1. Cannot copy to self ──────────────────────────────────────────
    if (sourceTrainNumber.trim().equalsIgnoreCase(targetTrainNumber.trim())) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "COPY_SAME_TRAIN",
        "Source and target train cannot be the same.");
    }

    // ── 2. Validate both trains exist and are active ────────────────────
    TrainEntity source = findActiveTrain(sourceTrainNumber);
    TrainEntity target = findActiveTrain(targetTrainNumber);

    // ── 3. Source must have coaches to copy ──────────────────────────────
    List<TrainCoachEntity> sourceCoaches =
      trainCoachRepository.findAllByTrainId(source.getTrainId());

    if (sourceCoaches.isEmpty()) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "SOURCE_HAS_NO_COACHES",
        "Train " + sourceTrainNumber + " has no coaches configured. Nothing to copy.");
    }

    // ── 4. Target must have NO coaches (bookings may exist) ─────────────
    int targetCoachCount = trainCoachRepository.countByTrain_TrainId(target.getTrainId());
    if (targetCoachCount > 0) {
      throw new BaseException(HttpStatus.CONFLICT, "TARGET_HAS_COACHES",
        "Train " + targetTrainNumber + " already has " + targetCoachCount +
          " coach type(s) configured. Cannot overwrite — bookings may already exist.");
    }

    // ── 5. Copy each coach row ──────────────────────────────────────────
    Long adminId = SecurityUtils.getCurrentAdminId();

    List<TrainCoachEntity> copied = sourceCoaches.stream()
      .map(src -> TrainCoachEntity.builder()
        .train(target)
        .coachType(src.getCoachType())
        .coachCount(src.getCoachCount())
        .tatkalSeats(src.getTatkalSeats())
        .racSeats(src.getRacSeats())
        .waitlistLimit(src.getWaitlistLimit())
        .effectiveFrom(LocalDate.now())
        .createdBy(adminId)
        .build())
      .toList();

    List<TrainCoachEntity> saved = trainCoachRepository.saveAll(copied);

    log.info("Copied {} coach types from train {} to train {}",
      saved.size(), sourceTrainNumber, targetTrainNumber);

    List<TrainCoachResponse> responses = saved.stream()
      .map(e -> toResponse(e, null))
      .toList();

    return TrainCopyCoachesResponse.builder()
      .sourceTrainNumber(sourceTrainNumber)
      .targetTrainNumber(targetTrainNumber)
      .copiedCount(saved.size())
      .coaches(responses)
      .message("Successfully copied " + saved.size() + " coach type(s) from train " +
        sourceTrainNumber + " to train " + targetTrainNumber + ".")
      .build();
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private TrainEntity findActiveTrain(String trainNumber) {
    TrainEntity train = findTrainByNumber(trainNumber);
    LocalDate today = LocalDate.now();
    if (!trainPeriodRepository.isActiveOnDate(train.getTrainId(), today))
      throw new BaseException(HttpStatus.BAD_REQUEST, "TRAIN_INACTIVE",
        "Train " + trainNumber + " is inactive (no active period covers today).");
    return train;
  }

  private TrainEntity findTrainByNumber(String trainNumber) {
    return trainRepository.findByTrainNumber(trainNumber.trim())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_NOT_FOUND",
        "Train not found: " + trainNumber));
  }

  private CoachTypeEntity findActiveCoachType(String typeCode) {
    CoachTypeEntity ct = coachTypeRepository.findByTypeCode(typeCode.trim().toUpperCase())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "COACH_TYPE_NOT_FOUND",
        "Coach type not found: " + typeCode));
    LocalDate today = LocalDate.now();
    if (!coachTypeRepository.findAllByTypeIdInAndActiveOnDate(List.of(ct.getTypeId()), today)
      .contains(ct))
      throw new BaseException(HttpStatus.BAD_REQUEST, "COACH_TYPE_INACTIVE",
        "Coach type '" + typeCode + "' is inactive (no active period covers today).");
    return ct;
  }

  private TrainCoachEntity findCoach(Long coachId, Long trainId) {
    return trainCoachRepository.findByCoachIdAndTrain_TrainId(coachId, trainId)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "COACH_NOT_FOUND",
        "Coach not found on this train."));
  }

  // tatkal + rac combined must not exceed total seats per coach
  private void validatePerCoachQuotas(int tatkal, int rac, int total, String typeCode) {
    if (tatkal > total)
      throw new BaseException(HttpStatus.BAD_REQUEST, "TATKAL_EXCEEDS_TOTAL",
        "Tatkal seats (" + tatkal + ") cannot exceed total seats per coach (" +
          total + ") for type '" + typeCode + "'.");
    if (rac > total)
      throw new BaseException(HttpStatus.BAD_REQUEST, "RAC_EXCEEDS_TOTAL",
        "RAC seats (" + rac + ") cannot exceed total seats per coach (" +
          total + ") for type '" + typeCode + "'.");
    if ((tatkal + rac) > total)
      throw new BaseException(HttpStatus.BAD_REQUEST, "QUOTA_OVERLAP",
        "Tatkal (" + tatkal + ") + RAC (" + rac + ") = " + (tatkal + rac) +
          " exceeds total seats per coach (" + total + ") for type '" + typeCode + "'.");
  }

  // ── Mapper ────────────────────────────────────────────────────────────────
  private TrainCoachResponse toResponse(TrainCoachEntity e, String message) {
    int totalSeats = e.getCoachType().getTotalSeats();
    int count      = e.getCoachCount();

    return TrainCoachResponse.builder()
      .coachId(e.getCoachId())
      .trainId(e.getTrain().getTrainId())
      .trainNumber(e.getTrain().getTrainNumber())
      .trainName(e.getTrain().getTrainName())
      .coachTypeId(e.getCoachType().getTypeId())
      .coachTypeCode(e.getCoachType().getTypeCode())
      .coachTypeName(e.getCoachType().getTypeName())
      .isAc(e.getCoachType().getIsAc())
      .totalSeats(totalSeats)
      .coachCount(count)
      .tatkalSeats(e.getTatkalSeats())
      .racSeats(e.getRacSeats())
      .waitlistLimit(e.getWaitlistLimit())
      .totalCoachSeats(count * totalSeats)
      .totalTatkalSeats(count * e.getTatkalSeats())
      .totalRacSeats(count * e.getRacSeats())
      .isActive(e.isCurrentlyActive())
      .effectiveFrom(e.getEffectiveFrom())
      .effectiveTill(e.getEffectiveTo())
      .createdBy(e.getCreatedBy())
      .updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt())
      .updatedAt(e.getUpdatedAt())
      .message(message)
      .build();
  }
}
