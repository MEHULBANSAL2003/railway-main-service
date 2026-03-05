package com.railway.main_service.service.coachTypeService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.coachType.AddCoachTypeRequest;
import com.railway.main_service.dto.request.coachType.UpdateCoachTypeRequest;
import com.railway.main_service.dto.response.coachType.CoachTypeResponse;
import com.railway.main_service.entity.CoachTypeEntity;
import com.railway.main_service.repository.CoachTypeRepository;
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
public class CoachTypeServiceImpl implements CoachTypeService {

  private final CoachTypeRepository coachTypeRepository;

  @Override
  @Transactional
  public CoachTypeResponse addCoachType(AddCoachTypeRequest request) {

    String typeCode = request.getTypeCode().trim().toUpperCase();

    if (coachTypeRepository.existsByTypeCode(typeCode)) {
      throw new BaseException(HttpStatus.CONFLICT, "COACH_TYPE_CODE_EXISTS",
        "Coach type with code '" + typeCode + "' already exists.");
    }

    if (coachTypeRepository.existsByTypeName(request.getTypeName().trim())) {
      throw new BaseException(HttpStatus.CONFLICT, "COACH_TYPE_NAME_EXISTS",
        "Coach type with name '" + request.getTypeName() + "' already exists.");
    }

    CoachTypeEntity entity = CoachTypeEntity.builder()
      .typeCode(typeCode)
      .typeName(request.getTypeName().trim())
      .description(request.getDescription() != null ? request.getDescription().trim() : null)
      .totalSeats(request.getTotalSeats())
      .isAc(request.getIsAc())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    CoachTypeEntity saved = coachTypeRepository.save(entity);
    return toResponse(saved, "Coach type added successfully.");
  }

  @Override
  @Transactional
  public CoachTypeResponse updateCoachType(String typeCode, UpdateCoachTypeRequest request) {

    CoachTypeEntity entity = coachTypeRepository.findByTypeCode(typeCode.toUpperCase())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "COACH_TYPE_NOT_FOUND",
        "Coach type not found with code: " + typeCode));

    if (request.getTypeName() != null && !request.getTypeName().isBlank()) {
      if (coachTypeRepository.existsByTypeNameAndTypeCodeNot(
        request.getTypeName().trim(), typeCode.toUpperCase())) {
        throw new BaseException(HttpStatus.CONFLICT, "COACH_TYPE_NAME_EXISTS",
          "Another coach type with name '" + request.getTypeName() + "' already exists.");
      }
      entity.setTypeName(request.getTypeName().trim());
    }

    if (request.getDescription() != null)
      entity.setDescription(request.getDescription().trim());

    if (request.getTotalSeats() != null)
      entity.setTotalSeats(request.getTotalSeats());

    if (request.getIsAc() != null)
      entity.setIsAc(request.getIsAc());

    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    CoachTypeEntity saved = coachTypeRepository.save(entity);
    return toResponse(saved, "Coach type updated successfully.");
  }

  @Override
  @Transactional
  public CoachTypeResponse toggleStatus(String typeCode, boolean isActive) {

    CoachTypeEntity entity = coachTypeRepository.findByTypeCode(typeCode.toUpperCase())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "COACH_TYPE_NOT_FOUND",
        "Coach type not found with code: " + typeCode));

    if (entity.getIsActive().equals(isActive))
      return toResponse(entity, null);

    entity.setIsActive(isActive);
    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    CoachTypeEntity saved = coachTypeRepository.save(entity);
    return toResponse(saved,
      "Coach type " + (isActive ? "activated" : "deactivated") + " successfully.");
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

  private CoachTypeResponse toResponse(CoachTypeEntity e, String message) {
    return CoachTypeResponse.builder()
      .typeId(e.getTypeId())
      .typeCode(e.getTypeCode())
      .typeName(e.getTypeName())
      .description(e.getDescription())
      .totalSeats(e.getTotalSeats())
      .isAc(e.getIsAc())
      .isActive(e.getIsActive())
      .createdBy(e.getCreatedBy())
      .updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt())
      .updatedAt(e.getUpdatedAt())
      .message(message)
      .build();
  }
}
