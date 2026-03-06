package com.railway.main_service.service.quotaService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.quota.AddQuotaRequest;
import com.railway.main_service.dto.request.quota.UpdateQuotaRequest;
import com.railway.main_service.dto.response.quota.QuotaResponse;
import com.railway.main_service.entity.QuotaEntity;
import com.railway.main_service.repository.QuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @Loggable @Slf4j @RequiredArgsConstructor
public class QuotaServiceImpl implements QuotaService {

  private final QuotaRepository quotaRepository;

  @Override @Transactional
  public QuotaResponse addQuota(AddQuotaRequest request) {
    String code = request.getQuotaCode().trim().toUpperCase();

    if (quotaRepository.existsByQuotaCode(code))
      throw new BaseException(HttpStatus.CONFLICT, "QUOTA_CODE_EXISTS",
        "Quota with code '" + code + "' already exists.");

    if (quotaRepository.existsByQuotaName(request.getQuotaName().trim()))
      throw new BaseException(HttpStatus.CONFLICT, "QUOTA_NAME_EXISTS",
        "Quota with name '" + request.getQuotaName() + "' already exists.");

    QuotaEntity saved = quotaRepository.save(QuotaEntity.builder()
      .quotaCode(code)
      .quotaName(request.getQuotaName().trim())
      .description(request.getDescription() != null ? request.getDescription().trim() : null)
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build());

    return toResponse(saved, "Quota added successfully.");
  }

  @Override @Transactional
  public QuotaResponse updateQuota(String quotaCode, UpdateQuotaRequest request) {
    QuotaEntity entity = findByCode(quotaCode);

    if (request.getQuotaName() != null && !request.getQuotaName().isBlank()) {
      if (quotaRepository.existsByQuotaNameAndQuotaCodeNot(
        request.getQuotaName().trim(), quotaCode.toUpperCase()))
        throw new BaseException(HttpStatus.CONFLICT, "QUOTA_NAME_EXISTS",
          "Another quota with name '" + request.getQuotaName() + "' already exists.");
      entity.setQuotaName(request.getQuotaName().trim());
    }
    if (request.getDescription() != null)
      entity.setDescription(request.getDescription().trim());

    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    return toResponse(quotaRepository.save(entity), "Quota updated successfully.");
  }

  @Override @Transactional
  public QuotaResponse toggleStatus(String quotaCode, boolean isActive) {
    QuotaEntity entity = findByCode(quotaCode);
    if (entity.getIsActive().equals(isActive)) return toResponse(entity, null);
    entity.setIsActive(isActive);
    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    return toResponse(quotaRepository.save(entity),
      "Quota " + (isActive ? "activated" : "deactivated") + " successfully.");
  }

  @Override
  public List<QuotaResponse> getAllForDropdown() {
    return quotaRepository.findAllByIsActiveTrueOrderByQuotaCodeAsc()
      .stream().map(e -> toResponse(e, null)).toList();
  }

  @Override
  public List<QuotaResponse> getAllForAdmin() {
    return quotaRepository.findAllByOrderByQuotaCodeAsc()
      .stream().map(e -> toResponse(e, null)).toList();
  }

  private QuotaEntity findByCode(String code) {
    return quotaRepository.findByQuotaCode(code.toUpperCase())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "QUOTA_NOT_FOUND",
        "Quota not found with code: " + code));
  }

  private QuotaResponse toResponse(QuotaEntity e, String message) {
    return QuotaResponse.builder()
      .quotaId(e.getQuotaId())
      .quotaCode(e.getQuotaCode())
      .quotaName(e.getQuotaName())
      .description(e.getDescription())
      .isActive(e.getIsActive())
      .createdBy(e.getCreatedBy())
      .updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt())
      .updatedAt(e.getUpdatedAt())
      .message(message)
      .build();
  }
}
