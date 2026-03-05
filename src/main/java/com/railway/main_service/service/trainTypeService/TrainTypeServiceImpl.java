package com.railway.main_service.service.trainTypeService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.trainType.AddTrainTypeRequest;
import com.railway.main_service.dto.request.trainType.UpdateTrainTypeRequest;
import com.railway.main_service.dto.response.trainType.TrainTypeResponse;
import com.railway.main_service.entity.TrainTypeEntity;
import com.railway.main_service.repository.TrainTypeRepository;
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
public class TrainTypeServiceImpl implements TrainTypeService {

  private final TrainTypeRepository trainTypeRepository;

  @Override
  @Transactional
  public TrainTypeResponse addTrainType(AddTrainTypeRequest request) {

    String typeCode = request.getTypeCode().trim().toUpperCase();

    if (trainTypeRepository.existsByTypeCode(typeCode)) {
      throw new BaseException(HttpStatus.CONFLICT, "TRAIN_TYPE_CODE_EXISTS",
        "Train type with code '" + typeCode + "' already exists.");
    }

    if (trainTypeRepository.existsByTypeName(request.getTypeName().trim())) {
      throw new BaseException(HttpStatus.CONFLICT, "TRAIN_TYPE_NAME_EXISTS",
        "Train type with name '" + request.getTypeName() + "' already exists.");
    }

    TrainTypeEntity entity = TrainTypeEntity.builder()
      .typeCode(typeCode)
      .typeName(request.getTypeName().trim())
      .description(request.getDescription() != null ? request.getDescription().trim() : null)
      .typicalSpeedKmh(request.getTypicalSpeedKmh())
      .isSuperfast(request.getIsSuperfast())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    TrainTypeEntity saved = trainTypeRepository.save(entity);
    return toResponse(saved, "Train type added successfully.");
  }

  @Override
  @Transactional
  public TrainTypeResponse updateTrainType(String typeCode, UpdateTrainTypeRequest request) {

    TrainTypeEntity entity = trainTypeRepository.findByTypeCode(typeCode.toUpperCase())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_TYPE_NOT_FOUND",
        "Train type not found with code: " + typeCode));

    // Check name conflict (exclude self)
    if (request.getTypeName() != null && !request.getTypeName().isBlank()) {
      if (trainTypeRepository.existsByTypeNameAndTypeCodeNot(
        request.getTypeName().trim(), typeCode.toUpperCase())) {
        throw new BaseException(HttpStatus.CONFLICT, "TRAIN_TYPE_NAME_EXISTS",
          "Another train type with name '" + request.getTypeName() + "' already exists.");
      }
      entity.setTypeName(request.getTypeName().trim());
    }

    if (request.getDescription() != null)
      entity.setDescription(request.getDescription().trim());

    if (request.getTypicalSpeedKmh() != null)
      entity.setTypicalSpeedKmh(request.getTypicalSpeedKmh());

    if (request.getIsSuperfast() != null)
      entity.setIsSuperfast(request.getIsSuperfast());

    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    TrainTypeEntity saved = trainTypeRepository.save(entity);
    return toResponse(saved, "Train type updated successfully.");
  }

  @Override
  @Transactional
  public TrainTypeResponse toggleStatus(String typeCode, boolean isActive) {

    TrainTypeEntity entity = trainTypeRepository.findByTypeCode(typeCode.toUpperCase())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_TYPE_NOT_FOUND",
        "Train type not found with code: " + typeCode));

    if (entity.getIsActive().equals(isActive)) {
      return toResponse(entity, null); // no change
    }

    entity.setIsActive(isActive);
    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    TrainTypeEntity saved = trainTypeRepository.save(entity);
    return toResponse(saved,
      "Train type " + (isActive ? "activated" : "deactivated") + " successfully.");
  }

  @Override
  public List<TrainTypeResponse> getAllForDropdown() {
    return trainTypeRepository.findAllByIsActiveTrueOrderByTypeCodeAsc()
      .stream().map(e -> toResponse(e, null)).toList();
  }

  @Override
  public List<TrainTypeResponse> getAllForAdmin(String search) {
    String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
    return trainTypeRepository.findAllForAdmin(searchTerm)
      .stream().map(e -> toResponse(e, null)).toList();
  }

  // ── Mapper ───────────────────────────────────────────────
  private TrainTypeResponse toResponse(TrainTypeEntity e, String message) {
    return TrainTypeResponse.builder()
      .typeId(e.getTypeId())
      .typeCode(e.getTypeCode())
      .typeName(e.getTypeName())
      .description(e.getDescription())
      .typicalSpeedKmh(e.getTypicalSpeedKmh())
      .isSuperfast(e.getIsSuperfast())
      .isActive(e.getIsActive())
      .createdBy(e.getCreatedBy())
      .updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt())
      .updatedAt(e.getUpdatedAt())
      .message(message)
      .build();
  }
}
