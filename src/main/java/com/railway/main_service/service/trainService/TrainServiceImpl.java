package com.railway.main_service.service.trainService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.train.AddTrainRequest;
import com.railway.main_service.dto.request.train.UpdateTrainRequest;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.train.ReturnTrainResponse;
import com.railway.main_service.dto.response.train.TrainResponse;
import com.railway.main_service.entity.TrainEntity;
import com.railway.main_service.entity.TrainTypeEntity;
import com.railway.main_service.entity.ZoneEntity;
import com.railway.main_service.repository.TrainRepository;
import com.railway.main_service.repository.TrainTypeRepository;
import com.railway.main_service.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Loggable
@Slf4j
@RequiredArgsConstructor
public class TrainServiceImpl implements TrainService {

  private final TrainRepository     trainRepository;
  private final TrainTypeRepository trainTypeRepository;
  private final ZoneRepository      zoneRepository;

  // ── Add ───────────────────────────────────────────────
  @Override
  @Transactional
  public TrainResponse addTrain(AddTrainRequest request) {

    String trainNumber = request.getTrainNumber().trim();
    String trainName   = request.getTrainName().trim();
    String typeCode    = request.getTrainTypeCode().trim().toUpperCase();
    String zoneCode    = request.getZoneCode().trim().toUpperCase();

    // Train number is the master identifier — must be globally unique
    if (trainRepository.existsByTrainNumber(trainNumber))
      throw new BaseException(HttpStatus.CONFLICT, "TRAIN_NUMBER_EXISTS",
        "Train number '" + trainNumber + "' already exists.");

    // Train name must also be unique — two trains can't have same name
    if (trainRepository.existsByTrainName(trainName))
      throw new BaseException(HttpStatus.CONFLICT, "TRAIN_NAME_EXISTS",
        "Train with name '" + trainName + "' already exists.");

    // Train type must exist and be active
    // Inactive type means this category is discontinued — don't allow new trains
    TrainTypeEntity trainType = trainTypeRepository.findByTypeCode(typeCode)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_TYPE_NOT_FOUND",
        "Train type not found: " + typeCode));

    if (!trainType.getIsActive())
      throw new BaseException(HttpStatus.BAD_REQUEST, "TRAIN_TYPE_INACTIVE",
        "Train type '" + typeCode + "' is inactive. Activate it before adding trains.");

    // Zone must exist and be active
    ZoneEntity zone = zoneRepository.findByCode(zoneCode)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND",
        "Zone not found: " + zoneCode));

    if (!zone.getIsActive())
      throw new BaseException(HttpStatus.BAD_REQUEST, "ZONE_INACTIVE",
        "Zone '" + zoneCode + "' is inactive.");

    TrainEntity entity = TrainEntity.builder()
      .trainNumber(trainNumber)
      .trainName(trainName)
      .trainType(trainType)
      .zone(zone)
      .pantrycar(request.getPantrycar())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    return toResponse(trainRepository.save(entity), "Train added successfully.");
  }

  // ── Update ────────────────────────────────────────────
  @Override
  @Transactional
  public TrainResponse updateTrain(String trainNumber, UpdateTrainRequest request) {

    TrainEntity entity = findByNumber(trainNumber);

    // Update name — check uniqueness excluding self
    if (request.getTrainName() != null && !request.getTrainName().isBlank()) {
      String newName = request.getTrainName().trim();
      if (trainRepository.existsByTrainNameAndTrainIdNot(newName, entity.getTrainId()))
        throw new BaseException(HttpStatus.CONFLICT, "TRAIN_NAME_EXISTS",
          "Another train with name '" + newName + "' already exists.");
      entity.setTrainName(newName);
    }

    // Zone transfer — allowed (trains do get transferred between zones)
    if (request.getZoneCode() != null && !request.getZoneCode().isBlank()) {
      String newZoneCode = request.getZoneCode().trim().toUpperCase();
      ZoneEntity zone = zoneRepository.findByCode(newZoneCode)
        .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND",
          "Zone not found: " + newZoneCode));
      if (!zone.getIsActive())
        throw new BaseException(HttpStatus.BAD_REQUEST, "ZONE_INACTIVE",
          "Zone '" + newZoneCode + "' is inactive.");
      entity.setZone(zone);
    }

    // Pantry car — can be physically added or removed from a train
    if (request.getPantrycar() != null)
      entity.setPantrycar(request.getPantrycar());

    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    return toResponse(trainRepository.save(entity), "Train updated successfully.");
  }

  // ── Toggle Status ─────────────────────────────────────
  @Override
  @Transactional
  public TrainResponse toggleStatus(String trainNumber, boolean isActive) {
    TrainEntity entity = findByNumber(trainNumber);

    if (entity.getIsActive().equals(isActive))
      return toResponse(entity, null);

    entity.setIsActive(isActive);
    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    trainRepository.save(entity);

    // NOTE: When you build TrainCoaches and Schedules,
    // add cascade deactivation here — same pattern as CoachType/TrainType.
    // For now only the train itself is toggled.
    String message = isActive
      ? "Train activated. Coaches and schedules were NOT auto-reactivated — re-enable them manually."
      : "Train deactivated.";

    return toResponse(entity, message);
  }

  // ── Cascade Info — for warning modal ─────────────────
  @Override
  public CascadeInfoResponse getCascadeInfo(String trainNumber) {
    TrainEntity entity = findByNumber(trainNumber);

    // For now cascade count is 0 — will include coach count + schedule count
    // once those tables are built. Return 0 so modal shows "safe to deactivate"
    // This method will be updated in the TrainCoaches implementation step.
    return CascadeInfoResponse.builder()
      .entityType("TRAIN")
      .entityCode(entity.getTrainNumber())
      .entityName(entity.getTrainName())
      .currentlyActive(entity.getIsActive())
      .activeFareRulesCount(0)
      .message("No linked records yet.")
      .build();
  }

  // ── Queries ───────────────────────────────────────────
  @Override
  public List<TrainResponse> getAllForAdmin(String search) {
    String s = (search != null && !search.isBlank()) ? search.trim() : null;
    return trainRepository.findAllForAdmin(s)
      .stream().map(e -> toResponse(e, null)).toList();
  }

  @Override
  public List<TrainResponse> getAllForDropdown(String search) {
    String s = (search != null && !search.isBlank()) ? search.trim() : null;
    return trainRepository.findActiveForDropdown(s)
      .stream().map(e -> toResponse(e, null)).toList();
  }

  // ── Private helpers ───────────────────────────────────
  private TrainEntity findByNumber(String trainNumber) {
    return trainRepository.findByTrainNumber(trainNumber.trim())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_NOT_FOUND",
        "Train not found: " + trainNumber));
  }

  private TrainResponse toResponse(TrainEntity e, String message) {
    return TrainResponse.builder()
      .trainId(e.getTrainId())
      .trainNumber(e.getTrainNumber())
      .trainName(e.getTrainName())
      .trainTypeCode(e.getTrainType().getTypeCode())
      .trainTypeName(e.getTrainType().getTypeName())
      .isSuperfast(e.getTrainType().getIsSuperfast())
      .zoneCode(e.getZone().getCode())
      .zoneName(e.getZone().getName())
      .pantrycar(e.getPantrycar())
      .isActive(e.getIsActive())
      .createdBy(e.getCreatedBy())
      .updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt())
      .updatedAt(e.getUpdatedAt())
      .message(message)
      .build();
  }

  // Add to TrainServiceImpl:

  @Override
  public ReturnTrainResponse getReturnTrainInfo(String trainNumber) {

    String trimmed = trainNumber.trim();

    // Validate — must be exactly 5 digits (already enforced on add, but be safe)
    if (!trimmed.matches("^[0-9]{5}$")) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_TRAIN_NUMBER",
        "Train number must be exactly 5 digits.");
    }

    int number = Integer.parseInt(trimmed);

    // Indian Railways convention:
    //   odd  number → return is number + 1  (e.g. 12951 → 12952)
    //   even number → return is number - 1  (e.g. 12952 → 12951)
    int returnNumber = (number % 2 != 0) ? number + 1 : number - 1;

    // Guard — returnNumber must stay in valid 5-digit range
    if (returnNumber < 10000 || returnNumber > 99999) {
      // Edge case: train like 10001 (odd) would give 10000 which is still valid
      // but 99999 (odd) would give 100000 which is invalid — handle gracefully
      return ReturnTrainResponse.builder()
        .returnTrainNumber(null)
        .exists(false)
        .existingTrain(null)
        .message("Return train number could not be determined for " + trimmed + ".")
        .build();
    }

    // Left-pad to 5 digits (e.g. returnNumber=10000 → "10000")
    String returnTrainNumber = String.valueOf(returnNumber);

    // Check if this return train already exists in DB
    return trainRepository.findByTrainNumber(returnTrainNumber)
      .map(existing -> ReturnTrainResponse.builder()
        .returnTrainNumber(returnTrainNumber)
        .exists(true)
        .existingTrain(toResponse(existing, null))
        .message("Return train " + returnTrainNumber + " (" + existing.getTrainName() +
          ") is already registered in the system.")
        .build())
      .orElseGet(() -> ReturnTrainResponse.builder()
        .returnTrainNumber(returnTrainNumber)
        .exists(false)
        .existingTrain(null)
        .message("Return train " + returnTrainNumber + " has not been added yet.")
        .build());
  }
}
