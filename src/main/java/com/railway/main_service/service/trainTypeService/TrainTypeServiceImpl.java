package com.railway.main_service.service.trainTypeService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.common.ChangeStatusRequest;
import com.railway.main_service.dto.request.trainType.AddTrainTypeRequest;
import com.railway.main_service.enums.ActiveStatus;
import com.railway.main_service.dto.request.trainType.SetAllowedCoachesRequest;
import com.railway.main_service.dto.request.trainType.UpdateTrainTypeRequest;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.trainType.AllowedCoachResponse;
import com.railway.main_service.dto.response.trainType.TrainTypeResponse;
import com.railway.main_service.entity.CoachTypeEntity;
import com.railway.main_service.entity.TrainTypeCoachEntity;
import com.railway.main_service.entity.TrainTypeEntity;
import com.railway.main_service.repository.CoachTypeRepository;
import com.railway.main_service.repository.FareRuleRepository;
import com.railway.main_service.repository.TrainTypeCoachRepository;
import com.railway.main_service.repository.TrainTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @Loggable @Slf4j @RequiredArgsConstructor
public class TrainTypeServiceImpl implements TrainTypeService {

  private final TrainTypeRepository      trainTypeRepository;
  private final FareRuleRepository       fareRuleRepository;
  private final TrainTypeCoachRepository trainTypeCoachRepository;
  private final CoachTypeRepository      coachTypeRepository;

  // ── Existing methods — UNCHANGED ─────────────────────────────────────────

  @Override
  public CascadeInfoResponse getCascadeInfo(String typeCode) {
    TrainTypeEntity entity = findByCode(typeCode);
    int activeRules = fareRuleRepository
      .countActiveByTrainTypeCode(typeCode.toUpperCase());
    int allowedCoaches = trainTypeCoachRepository.countByTrainType_TypeCode(typeCode.toUpperCase());
    return CascadeInfoResponse.builder()
      .entityType("TRAIN_TYPE")
      .entityCode(entity.getTypeCode())
      .entityName(entity.getTypeName())
      .currentlyActive(entity.isCurrentlyActive())
      .activeFareRulesCount(activeRules)
      .message(activeRules > 0
        ? activeRules + " active fare rule(s) will be deactivated. " +
        allowedCoaches + " allowed coach mapping(s) will remain unchanged."
        : "No active fare rules linked. " +
        allowedCoaches + " allowed coach mapping(s) will remain unchanged.")
      .build();
  }

  @Override
  @Transactional
  public TrainTypeResponse changeStatus(String typeCode, ChangeStatusRequest request) {
    TrainTypeEntity entity = findByCode(typeCode);

    String message;
    if (request.getStatus() == ActiveStatus.ACTIVE) {
      entity.setEffectiveFrom(request.getEffectiveFrom());
      entity.setEffectiveTill(null);
      entity.setReason(request.getReason());
      message = "Train type activated. Linked fare rules were NOT auto-reactivated — re-enable them manually.";
    } else {
      entity.setEffectiveTill(request.getEffectiveFrom());
      entity.setReason(request.getReason());
      int affected = fareRuleRepository
        .deactivateByTrainTypeCode(typeCode.toUpperCase(), SecurityUtils.getCurrentAdminId());
      log.info("CASCADE: {} fare rules deactivated for train type '{}' by admin {}",
        affected, typeCode, SecurityUtils.getCurrentAdminId());
      message = affected > 0
        ? "Train type deactivated. " + affected + " linked fare rule(s) also deactivated."
        : "Train type deactivated.";
    }
    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    trainTypeRepository.save(entity);
    return toResponse(entity, message);
  }

  @Override @Transactional
  public TrainTypeResponse addTrainType(AddTrainTypeRequest request) {
    String typeCode = request.getTypeCode().trim().toUpperCase();
    if (trainTypeRepository.existsByTypeCode(typeCode))
      throw new BaseException(HttpStatus.CONFLICT, "TRAIN_TYPE_CODE_EXISTS",
        "Train type with code '" + typeCode + "' already exists.");
    if (trainTypeRepository.existsByTypeName(request.getTypeName().trim()))
      throw new BaseException(HttpStatus.CONFLICT, "TRAIN_TYPE_NAME_EXISTS",
        "Train type with name '" + request.getTypeName() + "' already exists.");
    TrainTypeEntity entity = TrainTypeEntity.builder()
      .typeCode(typeCode).typeName(request.getTypeName().trim())
      .description(request.getDescription() != null ? request.getDescription().trim() : null)
      .typicalSpeedKmh(request.getTypicalSpeedKmh())
      .isSuperfast(request.getIsSuperfast())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();
    return toResponse(trainTypeRepository.save(entity), "Train type added successfully.");
  }

  @Override @Transactional
  public TrainTypeResponse updateTrainType(String typeCode, UpdateTrainTypeRequest request) {
    TrainTypeEntity entity = findByCode(typeCode);
    if (request.getTypeName() != null && !request.getTypeName().isBlank()) {
      if (trainTypeRepository.existsByTypeNameAndTypeCodeNot(
        request.getTypeName().trim(), typeCode.toUpperCase()))
        throw new BaseException(HttpStatus.CONFLICT, "TRAIN_TYPE_NAME_EXISTS",
          "Another train type with name '" + request.getTypeName() + "' already exists.");
      entity.setTypeName(request.getTypeName().trim());
    }
    if (request.getDescription()    != null) entity.setDescription(request.getDescription().trim());
    if (request.getTypicalSpeedKmh() != null) entity.setTypicalSpeedKmh(request.getTypicalSpeedKmh());
    if (request.getIsSuperfast()    != null) entity.setIsSuperfast(request.getIsSuperfast());
    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    return toResponse(trainTypeRepository.save(entity), "Train type updated successfully.");
  }

  @Override
  public List<TrainTypeResponse> getAllForDropdown(String search) {
    String s = (search != null && !search.isBlank()) ? search.trim() : null;
    return trainTypeRepository.findActiveForDropdown(s)
      .stream().map(e -> toResponse(e, null)).toList();
  }

  @Override
  public List<TrainTypeResponse> getAllForAdmin(String search) {
    String s = (search != null && !search.isBlank()) ? search.trim() : null;
    return trainTypeRepository.findAllForAdmin(s)
      .stream().map(e -> toResponse(e, null)).toList();
  }

  // ── NEW: Allowed coaches ──────────────────────────────────────────────────

  @Override
  public List<AllowedCoachResponse> getAllowedCoaches(String typeCode) {
    TrainTypeEntity trainType = findByCode(typeCode);
    return trainTypeCoachRepository.findAllByTrainTypeId(trainType.getTypeId())
      .stream().map(ttc -> toAllowedCoachResponse(ttc.getCoachType())).toList();
  }

  @Override
  @Transactional
  public List<AllowedCoachResponse> setAllowedCoaches(String typeCode,
                                                      SetAllowedCoachesRequest request) {
    TrainTypeEntity trainType = findByCode(typeCode);

    // Validate all coach type codes exist and are active
    List<CoachTypeEntity> coachTypes = request.getCoachTypeCodes().stream()
      .map(code -> {
        CoachTypeEntity ct = coachTypeRepository
          .findByTypeCode(code.trim().toUpperCase())
          .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "COACH_TYPE_NOT_FOUND",
            "Coach type not found: " + code));
        if (!ct.isCurrentlyActive())
          throw new BaseException(HttpStatus.BAD_REQUEST, "COACH_TYPE_INACTIVE",
            "Coach type '" + code + "' is inactive.");
        return ct;
      }).toList();

    // Full replace — delete existing mappings then re-insert
    trainTypeCoachRepository.deleteAllByTrainTypeId(trainType.getTypeId());

    List<TrainTypeCoachEntity> newMappings = coachTypes.stream()
      .map(ct -> TrainTypeCoachEntity.builder()
        .trainType(trainType)
        .coachType(ct)
        .createdBy(SecurityUtils.getCurrentAdminId())
        .build())
      .toList();

    trainTypeCoachRepository.saveAll(newMappings);

    log.info("Allowed coaches updated for train type '{}': {} → {}",
      typeCode, request.getCoachTypeCodes().size(), coachTypes.stream()
        .map(CoachTypeEntity::getTypeCode).toList());

    return coachTypes.stream().map(this::toAllowedCoachResponse).toList();
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private TrainTypeEntity findByCode(String code) {
    return trainTypeRepository.findByTypeCode(code.toUpperCase())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_TYPE_NOT_FOUND",
        "Train type not found: " + code));
  }

  private TrainTypeResponse toResponse(TrainTypeEntity e, String message) {
    return TrainTypeResponse.builder()
      .typeId(e.getTypeId()).typeCode(e.getTypeCode()).typeName(e.getTypeName())
      .description(e.getDescription()).typicalSpeedKmh(e.getTypicalSpeedKmh())
      .isSuperfast(e.getIsSuperfast()).isActive(e.isCurrentlyActive())
      .effectiveFrom(e.getEffectiveFrom()).effectiveTill(e.getEffectiveTill()).reason(e.getReason())
      .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).message(message)
      .build();
  }

  private AllowedCoachResponse toAllowedCoachResponse(CoachTypeEntity ct) {
    return AllowedCoachResponse.builder()
      .coachTypeId(ct.getTypeId())
      .coachTypeCode(ct.getTypeCode())
      .coachTypeName(ct.getTypeName())
      .totalSeats(ct.getTotalSeats())
      .isAc(ct.getIsAc())
      .isActive(ct.isCurrentlyActive())
      .effectiveFrom(ct.getEffectiveFrom())
      .effectiveTill(ct.getEffectiveTill())
      .reason(ct.getReason())
      .build();
  }
}
