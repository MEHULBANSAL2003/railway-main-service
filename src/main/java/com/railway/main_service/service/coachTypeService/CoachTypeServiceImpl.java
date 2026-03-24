package com.railway.main_service.service.coachTypeService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.coachType.AddCoachTypeRequest;
import com.railway.main_service.dto.request.coachType.UpdateCoachTypeRequest;
import com.railway.main_service.dto.request.common.ChangeStatusRequest;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.enums.ActiveStatus;
import com.railway.main_service.dto.response.coachType.CoachTypeResponse;
import com.railway.main_service.entity.CoachTypeEntity;
import com.railway.main_service.repository.CoachTypeRepository;
import com.railway.main_service.repository.FareRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @Loggable @Slf4j @RequiredArgsConstructor
public class CoachTypeServiceImpl implements CoachTypeService {

  private final CoachTypeRepository coachTypeRepository;
  private final FareRuleRepository  fareRuleRepository;   // injected for cascade

  // ── Get cascade info BEFORE toggling — used by frontend warning modal ──
  @Override
  public CascadeInfoResponse getCascadeInfo(String typeCode) {
    CoachTypeEntity entity = findByCode(typeCode);
    int activeRules = fareRuleRepository
      .countActiveByCoachTypeCode(typeCode.toUpperCase());
    return CascadeInfoResponse.builder()
      .entityType("COACH_TYPE")
      .entityCode(entity.getTypeCode())
      .entityName(entity.getTypeName())
      .currentlyActive(entity.isCurrentlyActive())
      .activeFareRulesCount(activeRules)
      .message(activeRules > 0
        ? activeRules + " active fare rule(s) will be deactivated."
        : "No active fare rules linked.")
      .build();
  }

  @Override
  @Transactional
  public CoachTypeResponse changeStatus(String typeCode, ChangeStatusRequest request) {
    CoachTypeEntity entity = findByCode(typeCode);

    if (request.getStatus() == ActiveStatus.ACTIVE) {
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTill(null);
        entity.setReason(request.getReason());
    } else {
        entity.setEffectiveTill(request.getEffectiveFrom());
        entity.setReason(request.getReason());
    }
    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    coachTypeRepository.save(entity);

    String message;
    if (request.getStatus() == ActiveStatus.INACTIVE) {
        int affected = fareRuleRepository
            .deactivateByCoachTypeCode(typeCode.toUpperCase(), SecurityUtils.getCurrentAdminId());
        log.info("CASCADE: {} fare rules deactivated for coach type '{}' by admin {}",
            affected, typeCode, SecurityUtils.getCurrentAdminId());
        message = affected > 0
            ? "Coach type deactivated. " + affected + " linked fare rule(s) also deactivated."
            : "Coach type deactivated.";
    } else {
        message = "Coach type activated. Linked fare rules were NOT auto-reactivated — re-enable them manually from Fare Rules page.";
    }

    return toResponse(entity, message);
  }

  // ── rest of methods unchanged ─────────────────────────────

  @Override @Transactional
  public CoachTypeResponse addCoachType(AddCoachTypeRequest request) {
    String typeCode = request.getTypeCode().trim().toUpperCase();
    if (coachTypeRepository.existsByTypeCode(typeCode))
      throw new BaseException(HttpStatus.CONFLICT, "COACH_TYPE_CODE_EXISTS",
        "Coach type with code '" + typeCode + "' already exists.");
    if (coachTypeRepository.existsByTypeName(request.getTypeName().trim()))
      throw new BaseException(HttpStatus.CONFLICT, "COACH_TYPE_NAME_EXISTS",
        "Coach type with name '" + request.getTypeName() + "' already exists.");
    CoachTypeEntity entity = CoachTypeEntity.builder()
      .typeCode(typeCode)
      .typeName(request.getTypeName().trim())
      .description(request.getDescription() != null ? request.getDescription().trim() : null)
      .totalSeats(request.getTotalSeats())
      .isAc(request.getIsAc())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();
    return toResponse(coachTypeRepository.save(entity), "Coach type added successfully.");
  }

  @Override @Transactional
  public CoachTypeResponse updateCoachType(String typeCode, UpdateCoachTypeRequest request) {
    CoachTypeEntity entity = findByCode(typeCode);
    if (request.getTypeName() != null && !request.getTypeName().isBlank()) {
      if (coachTypeRepository.existsByTypeNameAndTypeCodeNot(
        request.getTypeName().trim(), typeCode.toUpperCase()))
        throw new BaseException(HttpStatus.CONFLICT, "COACH_TYPE_NAME_EXISTS",
          "Another coach type with name '" + request.getTypeName() + "' already exists.");
      entity.setTypeName(request.getTypeName().trim());
    }
    if (request.getDescription() != null) entity.setDescription(request.getDescription().trim());
    if (request.getTotalSeats() != null)  entity.setTotalSeats(request.getTotalSeats());
    if (request.getIsAc() != null)        entity.setIsAc(request.getIsAc());
    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    return toResponse(coachTypeRepository.save(entity), "Coach type updated successfully.");
  }

  @Override
  public List<CoachTypeResponse> getAllForDropdown(String search) {
    String s = (search != null && !search.isBlank()) ? search.trim() : null;
    return coachTypeRepository.findActiveForDropdown(s)
      .stream().map(e -> toResponse(e, null)).toList();
  }

  @Override
  public List<CoachTypeResponse> getAllForAdmin(String search) {
    String s = (search != null && !search.isBlank()) ? search.trim() : null;
    return coachTypeRepository.findAllForAdmin(s)
      .stream().map(e -> toResponse(e, null)).toList();
  }

  private CoachTypeEntity findByCode(String code) {
    return coachTypeRepository.findByTypeCode(code.toUpperCase())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "COACH_TYPE_NOT_FOUND",
        "Coach type not found: " + code));
  }

  private CoachTypeResponse toResponse(CoachTypeEntity e, String message) {
    return CoachTypeResponse.builder()
      .typeId(e.getTypeId()).typeCode(e.getTypeCode()).typeName(e.getTypeName())
      .description(e.getDescription()).totalSeats(e.getTotalSeats()).isAc(e.getIsAc())
      .isActive(e.isCurrentlyActive()).effectiveFrom(e.getEffectiveFrom()).effectiveTill(e.getEffectiveTill()).reason(e.getReason())
      .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).message(message)
      .build();
  }
}
