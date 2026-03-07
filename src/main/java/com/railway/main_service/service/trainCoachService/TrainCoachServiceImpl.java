package com.railway.main_service.service.trainCoachService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.trainCoach.AddTrainCoachRequest;
import com.railway.main_service.dto.request.trainCoach.UpdateTrainCoachRequest;
import com.railway.main_service.dto.response.trainCoach.TrainCoachResponse;
import com.railway.main_service.entity.CoachTypeEntity;
import com.railway.main_service.entity.TrainCoachEntity;
import com.railway.main_service.entity.TrainEntity;
import com.railway.main_service.repository.CoachTypeRepository;
import com.railway.main_service.repository.TrainCoachRepository;
import com.railway.main_service.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @Loggable @Slf4j @RequiredArgsConstructor
public class TrainCoachServiceImpl implements TrainCoachService {

  private final TrainCoachRepository trainCoachRepository;
  private final TrainRepository      trainRepository;
  private final CoachTypeRepository  coachTypeRepository;

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

    if (req.getCoachCount()   != null) e.setCoachCount(req.getCoachCount());

    int newTatkal = req.getTatkalSeats()  != null ? req.getTatkalSeats()  : e.getTatkalSeats();
    int newRac    = req.getRacSeats()     != null ? req.getRacSeats()     : e.getRacSeats();

    validatePerCoachQuotas(newTatkal, newRac, totalSeats, e.getCoachType().getTypeCode());

    e.setTatkalSeats(newTatkal);
    e.setRacSeats(newRac);
    if (req.getWaitlistLimit() != null) e.setWaitlistLimit(req.getWaitlistLimit());
    e.setUpdatedBy(SecurityUtils.getCurrentAdminId());

    return toResponse(trainCoachRepository.save(e), "Coach updated successfully.");
  }

  // ── Toggle ────────────────────────────────────────────────────────────────
  @Override
  @Transactional
  public TrainCoachResponse toggleStatus(String trainNumber, Long coachId, boolean isActive) {
    TrainEntity      train = findActiveTrain(trainNumber);
    TrainCoachEntity e     = findCoach(coachId, train.getTrainId());

    if (e.getIsActive().equals(isActive)) return toResponse(e, null);
    e.setIsActive(isActive);
    e.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    trainCoachRepository.save(e);

    return toResponse(e, isActive ? "Coach activated." : "Coach deactivated.");
  }

  // ── Get all ───────────────────────────────────────────────────────────────
  @Override
  public List<TrainCoachResponse> getAllByTrain(String trainNumber) {
    TrainEntity train = findTrainByNumber(trainNumber);
    return trainCoachRepository.findAllByTrainId(train.getTrainId())
      .stream().map(e -> toResponse(e, null)).toList();
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private TrainEntity findActiveTrain(String trainNumber) {
    TrainEntity train = findTrainByNumber(trainNumber);
    if (!train.getIsActive())
      throw new BaseException(HttpStatus.BAD_REQUEST, "TRAIN_INACTIVE",
        "Train " + trainNumber + " is inactive.");
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
    if (!ct.getIsActive())
      throw new BaseException(HttpStatus.BAD_REQUEST, "COACH_TYPE_INACTIVE",
        "Coach type '" + typeCode + "' is inactive.");
    return ct;
  }

  private TrainCoachEntity findCoach(Long coachId, Long trainId) {
    return trainCoachRepository.findByCoachIdAndTrain_TrainId(coachId, trainId)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "COACH_NOT_FOUND",
        "Coach not found on this train."));
  }

  // tatkal + rac combined must not exceed total seats per coach
  // (a seat can't be both tatkal and RAC simultaneously)
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
      .isActive(e.getIsActive())
      .createdBy(e.getCreatedBy())
      .updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt())
      .updatedAt(e.getUpdatedAt())
      .message(message)
      .build();
  }
}
