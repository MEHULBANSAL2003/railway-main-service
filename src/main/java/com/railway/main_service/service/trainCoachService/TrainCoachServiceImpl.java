package com.railway.main_service.service.trainCoachService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.common.ChangeStatusRequest;
import com.railway.main_service.dto.request.trainCoach.AddTrainCoachRequest;
import com.railway.main_service.dto.request.trainCoach.UpdateTrainCoachRequest;
import com.railway.main_service.enums.ActiveStatus;
import com.railway.main_service.dto.response.trainCoach.CoachTypeDropdownResponse;
import com.railway.main_service.dto.response.trainCoach.TrainCoachResponse;
import com.railway.main_service.dto.response.trainCoach.TrainCopyCoachesResponse;
import com.railway.main_service.entity.CoachTypeEntity;
import com.railway.main_service.entity.TrainCoachEntity;
import com.railway.main_service.entity.TrainEntity;
import com.railway.main_service.repository.CoachTypeRepository;
import com.railway.main_service.repository.TrainCoachRepository;
import com.railway.main_service.repository.TrainRepository;
import com.railway.main_service.repository.TrainTypeCoachRepository;
import com.railway.main_service.service.inventoryService.InventoryInitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service @Loggable @Slf4j @RequiredArgsConstructor
public class TrainCoachServiceImpl implements TrainCoachService {

  private final TrainCoachRepository     trainCoachRepository;
  private final TrainRepository          trainRepository;
  private final CoachTypeRepository      coachTypeRepository;
  private final TrainTypeCoachRepository trainTypeCoachRepository;
  private final InventoryInitService     inventoryInitService;

  @Override
  @Transactional
  public TrainCoachResponse addCoach(String trainNumber, AddTrainCoachRequest req) {
    TrainEntity     train     = findActiveTrain(trainNumber);
    CoachTypeEntity coachType = findActiveCoachType(req.getCoachTypeCode());

    if (trainCoachRepository.existsActiveRowByTrainAndCoachType(
      train.getTrainId(), coachType.getTypeId()))
      throw new BaseException(HttpStatus.CONFLICT, "COACH_ALREADY_EXISTS",
        "Train " + trainNumber + " already has an active config for coach type '" +
          coachType.getTypeCode() + "'. Use 'Change Config' to modify it.");

    LocalDate effectiveFrom = req.getEffectiveFrom() != null
      ? req.getEffectiveFrom() : LocalDate.now();

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
      .effectiveFrom(effectiveFrom)
      .effectiveTill(req.getEffectiveTill())
      .reason(req.getReason())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    TrainCoachEntity saved = trainCoachRepository.save(entity);
    inventoryInitService.initForNewCoach(saved);
    return toResponse(saved, "Coach added successfully.");
  }

  @Override
  @Transactional
  public TrainCoachResponse updateCoach(String trainNumber, Long coachId,
                                        UpdateTrainCoachRequest req) {
    TrainEntity      train = findActiveTrain(trainNumber);
    TrainCoachEntity e     = findCoach(coachId, train.getTrainId());
    int totalSeats = e.getCoachType().getTotalSeats();

    if (req.getCoachCount() != null) e.setCoachCount(req.getCoachCount());

    int newTatkal = req.getTatkalSeats() != null ? req.getTatkalSeats() : e.getTatkalSeats();
    int newRac    = req.getRacSeats()    != null ? req.getRacSeats()    : e.getRacSeats();

    validatePerCoachQuotas(newTatkal, newRac, totalSeats, e.getCoachType().getTypeCode());

    e.setTatkalSeats(newTatkal);
    e.setRacSeats(newRac);
    if (req.getWaitlistLimit() != null) e.setWaitlistLimit(req.getWaitlistLimit());
    e.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    return toResponse(trainCoachRepository.save(e), "Coach updated successfully.");
  }

  @Override
  @Transactional
  public TrainCoachResponse changeStatus(String trainNumber, Long coachId, ChangeStatusRequest request) {
    TrainEntity      train = findActiveTrain(trainNumber);
    TrainCoachEntity e     = findCoach(coachId, train.getTrainId());
    if (request.getStatus() == ActiveStatus.ACTIVE) {
        e.setEffectiveFrom(request.getEffectiveFrom());
        e.setEffectiveTill(null);
        e.setReason(request.getReason());
    } else {
        e.setEffectiveTill(request.getEffectiveFrom());
        e.setReason(request.getReason());
    }
    e.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    trainCoachRepository.save(e);
    return toResponse(e, request.getStatus() == ActiveStatus.ACTIVE ? "Coach activated." : "Coach deactivated.");
  }

  @Override
  public List<TrainCoachResponse> getAllByTrain(String trainNumber) {
    TrainEntity train = findTrainByNumber(trainNumber);
    return trainCoachRepository
      .findCurrentByTrainId(train.getTrainId(), LocalDate.now())
      .stream().map(e -> toResponse(e, null)).toList();
  }

  @Override
  public List<TrainCoachResponse> getInactiveByTrain(String trainNumber) {
    TrainEntity train = findTrainByNumber(trainNumber);
    return trainCoachRepository
      .findInactiveByTrainId(train.getTrainId(), LocalDate.now())
      .stream().map(e -> toResponse(e, null)).toList();
  }

  @Override
  public List<TrainCoachResponse> getCoachHistory(String trainNumber, String coachTypeCode) {
    TrainEntity train = findTrainByNumber(trainNumber);
    return trainCoachRepository
      .findHistoryByTrainIdAndTypeCode(train.getTrainId(), coachTypeCode.toUpperCase())
      .stream().map(e -> toResponse(e, null)).toList();
  }

  private TrainEntity findActiveTrain(String trainNumber) {
    TrainEntity train = findTrainByNumber(trainNumber);
    if (!train.isCurrentlyActive())
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
    if (!ct.isCurrentlyActive())
      throw new BaseException(HttpStatus.BAD_REQUEST, "COACH_TYPE_INACTIVE",
        "Coach type '" + typeCode + "' is inactive.");
    return ct;
  }

  private TrainCoachEntity findCoach(Long coachId, Long trainId) {
    return trainCoachRepository.findByCoachIdAndTrain_TrainId(coachId, trainId)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "COACH_NOT_FOUND",
        "Coach not found on this train."));
  }

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
      .effectiveFrom(e.getEffectiveFrom())
      .effectiveTill(e.getEffectiveTill())
      .reason(e.getReason())
      .isActive(e.isCurrentlyActive())
      .createdBy(e.getCreatedBy())
      .updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt())
      .updatedAt(e.getUpdatedAt())
      .message(message)
      .build();
  }

  @Override
  public List<CoachTypeDropdownResponse> getAvailableCoachTypes(String trainNumber) {
    TrainEntity train = findTrainByNumber(trainNumber);

    List<Long> allowedIds = trainTypeCoachRepository
      .findAllowedCoachTypeIds(train.getTrainType().getTypeId());
    if (allowedIds.isEmpty()) return List.of();

    List<Long> usedIds = trainCoachRepository
      .findUsedCoachTypeIdsByTrainId(train.getTrainId());

    List<Long> availableIds = allowedIds.stream()
      .filter(id -> !usedIds.contains(id))
      .toList();
    if (availableIds.isEmpty()) return List.of();

    return coachTypeRepository.findAllActiveByTypeIdIn(availableIds).stream()
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

  @Override
  @Transactional
  public TrainCopyCoachesResponse copyCoaches(String sourceTrainNumber,
                                              String targetTrainNumber) {
    if (sourceTrainNumber.trim().equalsIgnoreCase(targetTrainNumber.trim()))
      throw new BaseException(HttpStatus.BAD_REQUEST, "COPY_SAME_TRAIN",
        "Source and target train cannot be the same.");

    TrainEntity source = findActiveTrain(sourceTrainNumber);
    TrainEntity target = findActiveTrain(targetTrainNumber);

    List<TrainCoachEntity> sourceCoaches =
      trainCoachRepository.findAllByTrainId(source.getTrainId());

    if (sourceCoaches.isEmpty())
      throw new BaseException(HttpStatus.BAD_REQUEST, "SOURCE_HAS_NO_COACHES",
        "Train " + sourceTrainNumber + " has no coaches configured. Nothing to copy.");

    int targetCoachCount = trainCoachRepository.countByTrain_TrainId(target.getTrainId());
    if (targetCoachCount > 0)
      throw new BaseException(HttpStatus.CONFLICT, "TARGET_HAS_COACHES",
        "Train " + targetTrainNumber + " already has " + targetCoachCount +
          " coach type(s) configured. Cannot overwrite — bookings may already exist.");

    Long adminId = SecurityUtils.getCurrentAdminId();

    List<TrainCoachEntity> copied = sourceCoaches.stream()
      .map(src -> TrainCoachEntity.builder()
        .train(target)
        .coachType(src.getCoachType())
        .coachCount(src.getCoachCount())
        .tatkalSeats(src.getTatkalSeats())
        .racSeats(src.getRacSeats())
        .waitlistLimit(src.getWaitlistLimit())
        .createdBy(adminId)
        .build())
      .toList();

    List<TrainCoachEntity> saved = trainCoachRepository.saveAll(copied);
    log.info("Copied {} coach types from train {} to train {}",
      saved.size(), sourceTrainNumber, targetTrainNumber);

    return TrainCopyCoachesResponse.builder()
      .sourceTrainNumber(sourceTrainNumber)
      .targetTrainNumber(targetTrainNumber)
      .copiedCount(saved.size())
      .coaches(saved.stream().map(e -> toResponse(e, null)).toList())
      .message("Successfully copied " + saved.size() + " coach type(s) from train " +
        sourceTrainNumber + " to train " + targetTrainNumber + ".")
      .build();
  }

  @Override
  public List<TrainCoachResponse> getAllByTrainIncludingInactive(String trainNumber) {
    TrainEntity train = findTrainByNumber(trainNumber);
    return trainCoachRepository.findAllWithHistoryByTrainId(train.getTrainId())
      .stream().map(e -> toResponse(e, null)).toList();
  }
}
