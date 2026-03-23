package com.railway.main_service.service.quotaService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.ActivateRequest;
import com.railway.main_service.dto.request.DeactivateRequest;
import com.railway.main_service.dto.request.quota.AddQuotaRequest;
import com.railway.main_service.dto.request.quota.UpdateQuotaRequest;
import com.railway.main_service.dto.response.DeactivationResponse;
import com.railway.main_service.dto.response.PeriodResponse;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.quota.QuotaResponse;
import com.railway.main_service.entity.QuotaEntity;
import com.railway.main_service.entity.QuotaPeriodEntity;
import com.railway.main_service.repository.FareRuleRepository;
import com.railway.main_service.repository.QuotaPeriodRepository;
import com.railway.main_service.repository.QuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service @Loggable @Slf4j @RequiredArgsConstructor
public class QuotaServiceImpl implements QuotaService {

  private final QuotaRepository       quotaRepository;
  private final QuotaPeriodRepository  quotaPeriodRepository;
  private final FareRuleRepository     fareRuleRepository;

  // ── Add ─────────────────────────────────────────────────────────────────────

  @Override @Transactional
  public QuotaResponse addQuota(AddQuotaRequest request) {
    String code = request.getQuotaCode().trim().toUpperCase();
    if (quotaRepository.existsByQuotaCode(code))
      throw new BaseException(HttpStatus.CONFLICT, "QUOTA_CODE_EXISTS",
        "Quota with code '" + code + "' already exists.");
    if (quotaRepository.existsByQuotaName(request.getQuotaName().trim()))
      throw new BaseException(HttpStatus.CONFLICT, "QUOTA_NAME_EXISTS",
        "Quota with name '" + request.getQuotaName() + "' already exists.");

    QuotaEntity entity = quotaRepository.save(QuotaEntity.builder()
      .quotaCode(code)
      .quotaName(request.getQuotaName().trim())
      .description(request.getDescription() != null ? request.getDescription().trim() : null)
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build());

    // Create initial open-ended period (active from today, no end)
    quotaPeriodRepository.save(QuotaPeriodEntity.builder()
      .quota(entity)
      .effectiveFrom(LocalDate.now())
      .effectiveTill(null)
      .reason("Initial creation")
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build());

    return toResponse(entity, true, "Quota added successfully.");
  }

  // ── Update ──────────────────────────────────────────────────────────────────

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
    entity = quotaRepository.save(entity);

    boolean active = quotaPeriodRepository.isActiveOnDate(entity.getQuotaId(), LocalDate.now());
    return toResponse(entity, active, "Quota updated successfully.");
  }

  // ── Deactivate ──────────────────────────────────────────────────────────────

  @Override @Transactional
  public DeactivationResponse deactivate(String quotaCode, DeactivateRequest request) {
    QuotaEntity entity = findByCode(quotaCode);
    LocalDate today = LocalDate.now();
    Long adminId = SecurityUtils.getCurrentAdminId();
    LocalDate fromDate = request.getFromDate();

    // Validate: cannot deactivate in the past
    if (fromDate.isBefore(today))
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_DATE",
        "Deactivation date cannot be in the past.");

    // Must have an open period to close
    boolean isActive = quotaPeriodRepository.isActiveOnDate(entity.getQuotaId(), fromDate);
    if (!isActive)
      throw new BaseException(HttpStatus.CONFLICT, "ALREADY_INACTIVE",
        "Quota '" + quotaCode + "' is not active on " + fromDate + ".");

    // Close the open period
    LocalDate closingDate = fromDate.equals(today) ? today : fromDate.minusDays(1);
    int closed = quotaPeriodRepository.closeOpenPeriod(entity.getQuotaId(), closingDate);
    log.info("Closed {} open period(s) for quota '{}' at {}", closed, quotaCode, closingDate);

    // Delete any future periods beyond the deactivation date
    int deletedFuture = quotaPeriodRepository.deleteFuturePeriods(entity.getQuotaId(), closingDate);
    if (deletedFuture > 0)
      log.info("Deleted {} future period(s) for quota '{}'", deletedFuture, quotaCode);

    // Cascade: end-date open fare rules
    int affectedRules = fareRuleRepository.endDateByQuotaCode(
      quotaCode.toUpperCase(), closingDate, adminId);
    log.info("CASCADE: {} fare rule(s) end-dated for quota '{}' by admin {}",
      affectedRules, quotaCode, adminId);

    // Build the period response for the closed period
    PeriodResponse periodResponse = quotaPeriodRepository
      .findActivePeriod(entity.getQuotaId(), closingDate)
      .map(this::toPeriodResponse)
      .orElse(null);

    String message = "Quota deactivated" +
      (affectedRules > 0 ? ". " + affectedRules + " linked fare rule(s) also end-dated." : ".");

    return DeactivationResponse.builder()
      .entityType("QUOTA")
      .entityCode(entity.getQuotaCode())
      .entityName(entity.getQuotaName())
      .action("DEACTIVATED")
      .period(periodResponse)
      .affectedFareRules(affectedRules)
      .message(message)
      .build();
  }

  // ── Activate ────────────────────────────────────────────────────────────────

  @Override @Transactional
  public DeactivationResponse activate(String quotaCode, ActivateRequest request) {
    QuotaEntity entity = findByCode(quotaCode);
    LocalDate today = LocalDate.now();
    Long adminId = SecurityUtils.getCurrentAdminId();
    LocalDate fromDate = request.getFromDate();

    // Validate: cannot activate in the past
    if (fromDate.isBefore(today))
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_DATE",
        "Activation date cannot be in the past.");

    // Must not already be active on that date
    boolean alreadyActive = quotaPeriodRepository.isActiveOnDate(entity.getQuotaId(), fromDate);
    if (alreadyActive)
      throw new BaseException(HttpStatus.CONFLICT, "ALREADY_ACTIVE",
        "Quota '" + quotaCode + "' is already active on " + fromDate + ".");

    // Create new period
    QuotaPeriodEntity newPeriod = quotaPeriodRepository.save(
      QuotaPeriodEntity.builder()
        .quota(entity)
        .effectiveFrom(fromDate)
        .effectiveTill(request.getTillDate())
        .reason(request.getReason())
        .createdBy(adminId)
        .build());

    PeriodResponse periodResponse = toPeriodResponse(newPeriod);

    String message = "Quota activated from " + fromDate +
      (request.getTillDate() != null ? " to " + request.getTillDate() : "") +
      ". Linked fare rules were NOT auto-reactivated — re-enable them manually from Fare Rules page.";

    return DeactivationResponse.builder()
      .entityType("QUOTA")
      .entityCode(entity.getQuotaCode())
      .entityName(entity.getQuotaName())
      .action("ACTIVATED")
      .period(periodResponse)
      .affectedFareRules(0)
      .message(message)
      .build();
  }

  // ── Get periods ─────────────────────────────────────────────────────────────

  @Override
  public List<PeriodResponse> getPeriods(String quotaCode) {
    QuotaEntity entity = findByCode(quotaCode);
    return quotaPeriodRepository.findAllByQuotaId(entity.getQuotaId())
      .stream().map(this::toPeriodResponse).toList();
  }

  // ── Cascade info — used by frontend warning modal ───────────────────────────

  @Override
  public CascadeInfoResponse getCascadeInfo(String quotaCode) {
    QuotaEntity entity = findByCode(quotaCode);
    LocalDate today = LocalDate.now();
    boolean active = quotaPeriodRepository.isActiveOnDate(entity.getQuotaId(), today);
    int activeRules = fareRuleRepository.countActiveByQuotaCodeOnDate(
      quotaCode.toUpperCase(), today);

    String msg = activeRules > 0
      ? activeRules + " active fare rule(s) will be end-dated."
      : "No active fare rules linked.";

    return CascadeInfoResponse.builder()
      .entityType("QUOTA")
      .entityCode(entity.getQuotaCode())
      .entityName(entity.getQuotaName())
      .currentlyActive(active)
      .activeFareRulesCount(activeRules)
      .message(msg)
      .build();
  }

  // ── Dropdown — only active on today ─────────────────────────────────────────

  @Override
  public List<QuotaResponse> getAllForDropdown() {
    LocalDate today = LocalDate.now();
    return quotaRepository.findActiveOnDate(today)
      .stream().map(e -> toResponse(e, true, null)).toList();
  }

  // ── Admin — all quotas with derived isActive ────────────────────────────────

  @Override
  public List<QuotaResponse> getAllForAdmin() {
    LocalDate today = LocalDate.now();
    return quotaRepository.findAllByOrderByQuotaCodeAsc()
      .stream().map(e -> {
        boolean active = quotaPeriodRepository.isActiveOnDate(e.getQuotaId(), today);
        List<PeriodResponse> periods = quotaPeriodRepository.findAllByQuotaId(e.getQuotaId())
          .stream().map(this::toPeriodResponse).toList();
        return toResponseWithPeriods(e, active, periods, null);
      }).toList();
  }

  // ── Private helpers ─────────────────────────────────────────────────────────

  private QuotaEntity findByCode(String code) {
    return quotaRepository.findByQuotaCode(code.toUpperCase())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "QUOTA_NOT_FOUND",
        "Quota not found: " + code));
  }

  private QuotaResponse toResponse(QuotaEntity e, boolean isActive, String message) {
    return QuotaResponse.builder()
      .quotaId(e.getQuotaId()).quotaCode(e.getQuotaCode()).quotaName(e.getQuotaName())
      .description(e.getDescription()).isActive(isActive)
      .createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).message(message)
      .build();
  }

  private QuotaResponse toResponseWithPeriods(QuotaEntity e, boolean isActive,
                                              List<PeriodResponse> periods, String message) {
    QuotaResponse resp = toResponse(e, isActive, message);
    resp.setPeriods(periods);
    // Derive effectiveFrom/Till from the current active period, if any
    if (periods != null) {
      periods.stream()
        .filter(p -> "ACTIVE".equals(p.getStatus()))
        .findFirst()
        .ifPresent(active -> {
          resp.setEffectiveFrom(active.getEffectiveFrom());
          resp.setEffectiveTill(active.getEffectiveTill());
        });
    }
    return resp;
  }

  private PeriodResponse toPeriodResponse(QuotaPeriodEntity p) {
    LocalDate today = LocalDate.now();
    String status;
    if (p.getEffectiveTill() != null && p.getEffectiveTill().isBefore(today)) status = "PAST";
    else if (p.getEffectiveFrom().isAfter(today)) status = "UPCOMING";
    else status = "ACTIVE";
    return PeriodResponse.builder()
      .periodId(p.getPeriodId())
      .effectiveFrom(p.getEffectiveFrom())
      .effectiveTill(p.getEffectiveTill())
      .status(status)
      .reason(p.getReason())
      .createdBy(p.getCreatedBy())
      .createdAt(p.getCreatedAt())
      .build();
  }
}
