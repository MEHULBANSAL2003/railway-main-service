package com.railway.main_service.service.coachTypeService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.ActivateRequest;
import com.railway.main_service.dto.request.DeactivateRequest;
import com.railway.main_service.dto.request.coachType.AddCoachTypeRequest;
import com.railway.main_service.dto.request.coachType.UpdateCoachTypeRequest;
import com.railway.main_service.dto.response.DeactivationResponse;
import com.railway.main_service.dto.response.PeriodResponse;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.coachType.CoachTypeResponse;
import com.railway.main_service.entity.CoachTypeEntity;
import com.railway.main_service.entity.CoachTypePeriodEntity;
import com.railway.main_service.repository.CoachTypePeriodRepository;
import com.railway.main_service.repository.CoachTypeRepository;
import com.railway.main_service.repository.FareRuleRepository;
import com.railway.main_service.repository.TrainCoachRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service @Loggable @Slf4j @RequiredArgsConstructor
public class CoachTypeServiceImpl implements CoachTypeService {

  private final CoachTypeRepository       coachTypeRepository;
  private final CoachTypePeriodRepository  coachTypePeriodRepository;
  private final FareRuleRepository         fareRuleRepository;
  private final TrainCoachRepository       trainCoachRepository;

  // ── Add ─────────────────────────────────────────────────────────────────────

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
    entity = coachTypeRepository.save(entity);

    // Create initial open-ended period (active from today, no end)
    coachTypePeriodRepository.save(CoachTypePeriodEntity.builder()
      .coachType(entity)
      .effectiveFrom(LocalDate.now())
      .effectiveTill(null)
      .reason("Initial creation")
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build());

    return toResponse(entity, true, "Coach type added successfully.");
  }

  // ── Update ──────────────────────────────────────────────────────────────────

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
    entity = coachTypeRepository.save(entity);

    boolean active = coachTypePeriodRepository.isActiveOnDate(entity.getTypeId(), LocalDate.now());
    return toResponse(entity, active, "Coach type updated successfully.");
  }

  // ── Deactivate ──────────────────────────────────────────────────────────────

  @Override @Transactional
  public DeactivationResponse deactivate(String typeCode, DeactivateRequest request) {
    CoachTypeEntity entity = findByCode(typeCode);
    LocalDate today = LocalDate.now();
    Long adminId = SecurityUtils.getCurrentAdminId();
    LocalDate fromDate = request.getFromDate();

    // Validate: cannot deactivate in the past
    if (fromDate.isBefore(today))
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_DATE",
        "Deactivation date cannot be in the past.");

    // BLOCK: if any trains have active coaches of this type on the deactivation date
    int activeCoaches = trainCoachRepository.countActiveByCoachTypeCodeOnDate(
      typeCode.toUpperCase(), fromDate);
    if (activeCoaches > 0)
      throw new BaseException(HttpStatus.CONFLICT, "COACH_TYPE_IN_USE",
        "Cannot deactivate: " + activeCoaches +
        " active train coach(es) still use this type on " + fromDate +
        ". Remove or end-date them first.");

    // Must have an open period to close
    boolean isActive = coachTypePeriodRepository.isActiveOnDate(entity.getTypeId(), fromDate);
    if (!isActive)
      throw new BaseException(HttpStatus.CONFLICT, "ALREADY_INACTIVE",
        "Coach type '" + typeCode + "' is not active on " + fromDate + ".");

    // Close the open period
    LocalDate closingDate = fromDate.equals(today) ? today : fromDate.minusDays(1);
    int closed = coachTypePeriodRepository.closeOpenPeriod(entity.getTypeId(), closingDate);
    log.info("Closed {} open period(s) for coach type '{}' at {}", closed, typeCode, closingDate);

    // Delete any future periods beyond the deactivation date
    int deletedFuture = coachTypePeriodRepository.deleteFuturePeriods(entity.getTypeId(), closingDate);
    if (deletedFuture > 0)
      log.info("Deleted {} future period(s) for coach type '{}'", deletedFuture, typeCode);

    // Cascade: end-date open fare rules
    int affectedRules = fareRuleRepository.endDateByCoachTypeCode(
      typeCode.toUpperCase(), closingDate, adminId);
    log.info("CASCADE: {} fare rule(s) end-dated for coach type '{}' by admin {}",
      affectedRules, typeCode, adminId);

    // Build the period response for the closed period
    PeriodResponse periodResponse = coachTypePeriodRepository
      .findActivePeriod(entity.getTypeId(), closingDate)
      .map(this::toPeriodResponse)
      .orElse(null);

    String message = "Coach type deactivated" +
      (affectedRules > 0 ? ". " + affectedRules + " linked fare rule(s) also end-dated." : ".");

    return DeactivationResponse.builder()
      .entityType("COACH_TYPE")
      .entityCode(entity.getTypeCode())
      .entityName(entity.getTypeName())
      .action("DEACTIVATED")
      .period(periodResponse)
      .affectedFareRules(affectedRules)
      .message(message)
      .build();
  }

  // ── Activate ────────────────────────────────────────────────────────────────

  @Override @Transactional
  public DeactivationResponse activate(String typeCode, ActivateRequest request) {
    CoachTypeEntity entity = findByCode(typeCode);
    LocalDate today = LocalDate.now();
    Long adminId = SecurityUtils.getCurrentAdminId();
    LocalDate fromDate = request.getFromDate();

    // Validate: cannot activate in the past
    if (fromDate.isBefore(today))
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_DATE",
        "Activation date cannot be in the past.");

    // Must not already be active on that date
    boolean alreadyActive = coachTypePeriodRepository.isActiveOnDate(entity.getTypeId(), fromDate);
    if (alreadyActive)
      throw new BaseException(HttpStatus.CONFLICT, "ALREADY_ACTIVE",
        "Coach type '" + typeCode + "' is already active on " + fromDate + ".");

    // Create new period
    CoachTypePeriodEntity newPeriod = coachTypePeriodRepository.save(
      CoachTypePeriodEntity.builder()
        .coachType(entity)
        .effectiveFrom(fromDate)
        .effectiveTill(request.getTillDate())
        .reason(request.getReason())
        .createdBy(adminId)
        .build());

    PeriodResponse periodResponse = toPeriodResponse(newPeriod);

    String message = "Coach type activated from " + fromDate +
      (request.getTillDate() != null ? " to " + request.getTillDate() : "") +
      ". Linked fare rules were NOT auto-reactivated — re-enable them manually from Fare Rules page.";

    return DeactivationResponse.builder()
      .entityType("COACH_TYPE")
      .entityCode(entity.getTypeCode())
      .entityName(entity.getTypeName())
      .action("ACTIVATED")
      .period(periodResponse)
      .affectedFareRules(0)
      .message(message)
      .build();
  }

  // ── Get periods ─────────────────────────────────────────────────────────────

  @Override
  public List<PeriodResponse> getPeriods(String typeCode) {
    CoachTypeEntity entity = findByCode(typeCode);
    return coachTypePeriodRepository.findAllByTypeId(entity.getTypeId())
      .stream().map(this::toPeriodResponse).toList();
  }

  // ── Cascade info — used by frontend warning modal ───────────────────────────

  @Override
  public CascadeInfoResponse getCascadeInfo(String typeCode) {
    CoachTypeEntity entity = findByCode(typeCode);
    LocalDate today = LocalDate.now();
    boolean active = coachTypePeriodRepository.isActiveOnDate(entity.getTypeId(), today);
    int activeRules = fareRuleRepository.countActiveByCoachTypeCodeOnDate(
      typeCode.toUpperCase(), today);
    int activeCoaches = trainCoachRepository.countActiveByCoachTypeCodeOnDate(
      typeCode.toUpperCase(), today);

    StringBuilder msg = new StringBuilder();
    if (activeRules > 0)
      msg.append(activeRules).append(" active fare rule(s) will be end-dated. ");
    if (activeCoaches > 0)
      msg.append(activeCoaches).append(" active train coach(es) use this type — deactivation will be BLOCKED.");
    if (msg.isEmpty())
      msg.append("No active fare rules or train coaches linked.");

    return CascadeInfoResponse.builder()
      .entityType("COACH_TYPE")
      .entityCode(entity.getTypeCode())
      .entityName(entity.getTypeName())
      .currentlyActive(active)
      .activeFareRulesCount(activeRules)
      .message(msg.toString().trim())
      .build();
  }

  // ── Dropdown — only active on today ─────────────────────────────────────────

  @Override
  public List<CoachTypeResponse> getAllForDropdown(String search) {
    String s = (search != null && !search.isBlank()) ? search.trim() : null;
    LocalDate today = LocalDate.now();
    return coachTypeRepository.findActiveForDropdown(s, today)
      .stream().map(e -> toResponse(e, true, null)).toList();
  }

  // ── Admin — all coach types with derived isActive ───────────────────────────

  @Override
  public List<CoachTypeResponse> getAllForAdmin(String search) {
    String s = (search != null && !search.isBlank()) ? search.trim() : null;
    LocalDate today = LocalDate.now();
    return coachTypeRepository.findAllForAdmin(s)
      .stream().map(e -> {
        boolean active = coachTypePeriodRepository.isActiveOnDate(e.getTypeId(), today);
        List<PeriodResponse> periods = coachTypePeriodRepository.findAllByTypeId(e.getTypeId())
          .stream().map(this::toPeriodResponse).toList();
        return toResponseWithPeriods(e, active, periods, null);
      }).toList();
  }

  // ── Private helpers ─────────────────────────────────────────────────────────

  private CoachTypeEntity findByCode(String code) {
    return coachTypeRepository.findByTypeCode(code.toUpperCase())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "COACH_TYPE_NOT_FOUND",
        "Coach type not found: " + code));
  }

  private CoachTypeResponse toResponse(CoachTypeEntity e, boolean isActive, String message) {
    return CoachTypeResponse.builder()
      .typeId(e.getTypeId()).typeCode(e.getTypeCode()).typeName(e.getTypeName())
      .description(e.getDescription()).totalSeats(e.getTotalSeats()).isAc(e.getIsAc())
      .isActive(isActive).createdBy(e.getCreatedBy()).updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).message(message)
      .build();
  }

  private CoachTypeResponse toResponseWithPeriods(CoachTypeEntity e, boolean isActive,
                                                  List<PeriodResponse> periods, String message) {
    CoachTypeResponse resp = toResponse(e, isActive, message);
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

  private PeriodResponse toPeriodResponse(CoachTypePeriodEntity p) {
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
