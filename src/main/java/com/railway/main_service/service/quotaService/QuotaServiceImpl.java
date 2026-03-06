package com.railway.main_service.service.quotaService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.quota.AddQuotaRequest;
import com.railway.main_service.dto.request.quota.UpdateQuotaRequest;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.quota.QuotaResponse;
import com.railway.main_service.entity.QuotaEntity;
import com.railway.main_service.repository.FareRuleRepository;
import com.railway.main_service.repository.QuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @Loggable @Slf4j @RequiredArgsConstructor
public class QuotaServiceImpl implements QuotaService {

  private final QuotaRepository    quotaRepository;
  private final FareRuleRepository fareRuleRepository;

  @Override
  public CascadeInfoResponse getCascadeInfo(String quotaCode) {
    QuotaEntity entity = findByCode(quotaCode);
    int activeRules = fareRuleRepository
      .countByQuota_QuotaCodeAndIsActiveTrue(quotaCode.toUpperCase());
    return CascadeInfoResponse.builder()
      .entityType("QUOTA")
      .entityCode(entity.getQuotaCode())
      .entityName(entity.getQuotaName())
      .currentlyActive(entity.getIsActive())
      .activeFareRulesCount(activeRules)
      .message(activeRules > 0
        ? activeRules + " active fare rule(s) will be deactivated."
        : "No active fare rules linked.")
      .build();
  }

  @Override @Transactional
  public QuotaResponse toggleStatus(String quotaCode, boolean isActive) {
    QuotaEntity entity = findByCode(quotaCode);

    if (entity.getIsActive().equals(isActive))
      return toResponse(entity, null);

    entity.setIsActive(isActive);
    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    quotaRepository.save(entity);

    String message;
    if (!isActive) {
      int affected = fareRuleRepository
        .deactivateByQuotaCode(quotaCode.toUpperCase(), SecurityUtils.getCurrentAdminId());
      log.info("CASCADE: {} fare rules deactivated for quota '{}' by admin {}",
        affected, quotaCode, SecurityUtils.getCurrentAdminId());
      message = affected > 0
        ? "Quota deactivated. " + affected + " linked fare rule(s) also deactivated."
        : "Quota deactivated.";
    } else {
      message = "Quota activated. Linked fare rules were NOT auto-reactivated — re-enable them manually from Fare Rules page.";
    }

    return toResponse(entity, message);
  }

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
      .quotaCode(code).quotaName(request.getQuotaName().trim())
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
          "Another quota with this name already exists.");
      entity.setQuotaName(request.getQuotaName().trim());
    }
    if (request.getDescription() != null)
      entity.setDescription(request.getDescription().trim());
    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    return toResponse(quotaRepository.save(entity), "Quota updated successfully.");
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
        "Quota not found: " + code));
  }

  private QuotaResponse toResponse(QuotaEntity e, String message) {
    return QuotaResponse.builder()
      .quotaId(e.getQuotaId()).quotaCode(e.getQuotaCode()).quotaName(e.getQuotaName())
      .description(e.getDescription()).isActive(e.getIsActive())
      .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).message(message)
      .build();
  }
}
