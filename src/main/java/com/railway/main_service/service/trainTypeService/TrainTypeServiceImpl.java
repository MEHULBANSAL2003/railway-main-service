package com.railway.main_service.service.trainTypeService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.ActivateRequest;
import com.railway.main_service.dto.request.DeactivateRequest;
import com.railway.main_service.dto.request.trainType.AddTrainTypeRequest;
import com.railway.main_service.dto.request.trainType.SetAllowedCoachesRequest;
import com.railway.main_service.dto.request.trainType.UpdateTrainTypeRequest;
import com.railway.main_service.dto.response.DeactivationResponse;
import com.railway.main_service.dto.response.PeriodResponse;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.trainType.AllowedCoachResponse;
import com.railway.main_service.dto.response.trainType.TrainTypeResponse;
import com.railway.main_service.entity.CoachTypeEntity;
import com.railway.main_service.entity.TrainTypeCoachEntity;
import com.railway.main_service.entity.TrainTypeEntity;
import com.railway.main_service.entity.TrainTypePeriodEntity;
import com.railway.main_service.repository.CoachTypeRepository;
import com.railway.main_service.repository.FareRuleRepository;
import com.railway.main_service.repository.TrainRepository;
import com.railway.main_service.repository.TrainTypeCoachRepository;
import com.railway.main_service.repository.TrainTypePeriodRepository;
import com.railway.main_service.repository.TrainTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service @Loggable @Slf4j @RequiredArgsConstructor
public class TrainTypeServiceImpl implements TrainTypeService {

  private final TrainTypeRepository       trainTypeRepository;
  private final TrainTypePeriodRepository  trainTypePeriodRepository;
  private final FareRuleRepository         fareRuleRepository;
  private final TrainRepository            trainRepository;
  private final TrainTypeCoachRepository   trainTypeCoachRepository;
  private final CoachTypeRepository        coachTypeRepository;
  private final com.railway.main_service.repository.CoachTypePeriodRepository coachTypePeriodRepository;

  // ── CRUD ────────────────────────────────────────────────────────────────────

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
      .typeCode(typeCode)
      .typeName(request.getTypeName().trim())
      .description(request.getDescription() != null ? request.getDescription().trim() : null)
      .typicalSpeedKmh(request.getTypicalSpeedKmh())
      .isSuperfast(request.getIsSuperfast())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();
    entity = trainTypeRepository.save(entity);

    // Create initial active period: effective from today, open-ended
    TrainTypePeriodEntity initialPeriod = TrainTypePeriodEntity.builder()
      .trainType(entity)
      .effectiveFrom(LocalDate.now())
      .effectiveTill(null)
      .reason("Created")
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();
    trainTypePeriodRepository.save(initialPeriod);

    return toResponse(entity, false, "Train type added successfully.");
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
    if (request.getDescription()     != null) entity.setDescription(request.getDescription().trim());
    if (request.getTypicalSpeedKmh() != null) entity.setTypicalSpeedKmh(request.getTypicalSpeedKmh());
    if (request.getIsSuperfast()     != null) entity.setIsSuperfast(request.getIsSuperfast());
    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    return toResponse(trainTypeRepository.save(entity), false, "Train type updated successfully.");
  }

  // ── Period-based deactivation ───────────────────────────────────────────────

  @Override @Transactional
  public DeactivationResponse deactivate(String typeCode, DeactivateRequest request) {
    TrainTypeEntity entity = findByCode(typeCode);
    LocalDate today    = LocalDate.now();
    LocalDate fromDate = request.getFromDate();
    LocalDate tillDate = request.getTillDate();
    Long adminId       = SecurityUtils.getCurrentAdminId();

    // Validate fromDate >= today
    if (fromDate.isBefore(today))
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_FROM_DATE",
        "fromDate cannot be in the past.");

    // Validate tillDate > fromDate (if provided)
    if (tillDate != null && !tillDate.isAfter(fromDate))
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_TILL_DATE",
        "tillDate must be after fromDate.");

    // Block if any train with active period references this type on fromDate
    int activeTrains = trainRepository.countActiveTrainsByTypeCode(typeCode.toUpperCase(), fromDate);
    if (activeTrains > 0)
      throw new BaseException(HttpStatus.CONFLICT, "ACTIVE_TRAINS_EXIST",
        "Cannot deactivate: " + activeTrains + " train(s) with an active period reference train type '"
          + typeCode + "' on " + fromDate + ". Deactivate or reassign those trains first.");

    // Close current open period: set effectiveTill = fromDate - 1
    trainTypePeriodRepository.closeOpenPeriod(entity.getTypeId(), fromDate.minusDays(1));

    // Delete any future periods that would conflict
    int deletedFuture = trainTypePeriodRepository.deleteFuturePeriods(entity.getTypeId(), fromDate.minusDays(1));
    if (deletedFuture > 0)
      log.info("Deleted {} future period(s) for train type '{}'", deletedFuture, typeCode);

    // If temporary deactivation (tillDate provided): create a future re-activation period
    PeriodResponse periodResponse;
    if (tillDate != null) {
      TrainTypePeriodEntity futurePeriod = TrainTypePeriodEntity.builder()
        .trainType(entity)
        .effectiveFrom(tillDate.plusDays(1))
        .effectiveTill(null)
        .reason("Auto-reactivation after temporary deactivation")
        .createdBy(adminId)
        .build();
      futurePeriod = trainTypePeriodRepository.save(futurePeriod);

      periodResponse = toPeriodResponse(futurePeriod, today);
    } else {
      // Permanent deactivation — no future period created
      periodResponse = PeriodResponse.builder()
        .effectiveFrom(fromDate)
        .effectiveTill(null)
        .status("DEACTIVATED")
        .reason(request.getReason())
        .createdBy(adminId)
        .build();
    }

    // CASCADE: close fare rules — set effectiveUntil = fromDate - 1
    int affectedFareRules = fareRuleRepository.endDateByTrainTypeCode(
      typeCode.toUpperCase(), fromDate.minusDays(1), adminId);
    log.info("CASCADE: {} fare rule(s) end-dated for train type '{}' by admin {}",
      affectedFareRules, typeCode, adminId);

    // Build warnings list
    List<String> warnings = new ArrayList<>();
    if (affectedFareRules > 0)
      warnings.add(affectedFareRules + " fare rule(s) will be end-dated.");
    if (deletedFuture > 0)
      warnings.add(deletedFuture + " future period(s) were removed.");

    String message = tillDate != null
      ? "Train type '" + typeCode + "' deactivated from " + fromDate + " to " + tillDate
        + ". Will auto-reactivate on " + tillDate.plusDays(1) + "."
      : "Train type '" + typeCode + "' deactivated from " + fromDate + " indefinitely.";

    return DeactivationResponse.builder()
      .entityType("TRAIN_TYPE")
      .entityCode(entity.getTypeCode())
      .entityName(entity.getTypeName())
      .action("DEACTIVATED")
      .period(periodResponse)
      .affectedFareRules(affectedFareRules)
      .warnings(warnings.isEmpty() ? null : warnings)
      .message(message)
      .build();
  }

  // ── Period-based activation ─────────────────────────────────────────────────

  @Override @Transactional
  public DeactivationResponse activate(String typeCode, ActivateRequest request) {
    TrainTypeEntity entity = findByCode(typeCode);
    LocalDate today    = LocalDate.now();
    LocalDate fromDate = request.getFromDate();
    LocalDate tillDate = request.getTillDate();
    Long adminId       = SecurityUtils.getCurrentAdminId();

    // Validate fromDate >= today
    if (fromDate.isBefore(today))
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_FROM_DATE",
        "fromDate cannot be in the past.");

    // Check for overlap with existing periods
    LocalDate overlapCheckTill = tillDate != null ? tillDate : LocalDate.of(9999, 12, 31);
    if (trainTypePeriodRepository.hasOverlap(entity.getTypeId(), fromDate, overlapCheckTill, null))
      throw new BaseException(HttpStatus.CONFLICT, "PERIOD_OVERLAP",
        "The requested activation period overlaps with an existing period.");

    // Create new active period
    TrainTypePeriodEntity newPeriod = TrainTypePeriodEntity.builder()
      .trainType(entity)
      .effectiveFrom(fromDate)
      .effectiveTill(tillDate)
      .reason(request.getReason())
      .createdBy(adminId)
      .build();
    newPeriod = trainTypePeriodRepository.save(newPeriod);

    log.info("Train type '{}' activated from {} to {} by admin {}",
      typeCode, fromDate, tillDate != null ? tillDate : "indefinitely", adminId);

    String message = tillDate != null
      ? "Train type '" + typeCode + "' activated from " + fromDate + " to " + tillDate + "."
      : "Train type '" + typeCode + "' activated from " + fromDate + " indefinitely.";

    return DeactivationResponse.builder()
      .entityType("TRAIN_TYPE")
      .entityCode(entity.getTypeCode())
      .entityName(entity.getTypeName())
      .action("ACTIVATED")
      .period(toPeriodResponse(newPeriod, today))
      .affectedFareRules(0)
      .message(message)
      .build();
  }

  // ── Get all periods for a train type ────────────────────────────────────────

  @Override
  public List<PeriodResponse> getPeriods(String typeCode) {
    TrainTypeEntity entity = findByCode(typeCode);
    LocalDate today = LocalDate.now();
    return trainTypePeriodRepository.findAllByTypeId(entity.getTypeId())
      .stream()
      .map(p -> toPeriodResponse(p, today))
      .toList();
  }

  // ── Cascade info (period-based) ─────────────────────────────────────────────

  @Override
  public CascadeInfoResponse getCascadeInfo(String typeCode) {
    TrainTypeEntity entity = findByCode(typeCode);
    LocalDate today = LocalDate.now();

    boolean currentlyActive = trainTypePeriodRepository.isActiveOnDate(entity.getTypeId(), today);
    int activeRules    = fareRuleRepository.countActiveByTrainTypeCodeOnDate(typeCode.toUpperCase(), today);
    int allowedCoaches = trainTypeCoachRepository.countByTrainType_TypeCode(typeCode.toUpperCase());

    return CascadeInfoResponse.builder()
      .entityType("TRAIN_TYPE")
      .entityCode(entity.getTypeCode())
      .entityName(entity.getTypeName())
      .currentlyActive(currentlyActive)
      .activeFareRulesCount(activeRules)
      .message(activeRules > 0
        ? activeRules + " active fare rule(s) will be end-dated. "
          + allowedCoaches + " allowed coach mapping(s) will remain unchanged."
        : "No active fare rules linked. "
          + allowedCoaches + " allowed coach mapping(s) will remain unchanged.")
      .build();
  }

  // ── Listings ────────────────────────────────────────────────────────────────

  @Override
  public List<TrainTypeResponse> getAllForDropdown(String search) {
    String s = (search != null && !search.isBlank()) ? search.trim() : null;
    return trainTypeRepository.findActiveForDropdown(s, LocalDate.now())
      .stream().map(e -> toResponse(e, false, null)).toList();
  }

  @Override
  public List<TrainTypeResponse> getAllForAdmin(String search) {
    String s = (search != null && !search.isBlank()) ? search.trim() : null;
    return trainTypeRepository.findAllForAdmin(s)
      .stream().map(e -> toResponse(e, true, null)).toList();
  }

  // ── Allowed coaches ─────────────────────────────────────────────────────────

  @Override
  public List<AllowedCoachResponse> getAllowedCoaches(String typeCode) {
    TrainTypeEntity trainType = findByCode(typeCode);
    return trainTypeCoachRepository.findAllByTrainTypeId(trainType.getTypeId())
      .stream().map(ttc -> toAllowedCoachResponse(ttc.getCoachType())).toList();
  }

  @Override @Transactional
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
        if (!coachTypePeriodRepository.isActiveOnDate(ct.getTypeId(), LocalDate.now()))
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

    log.info("Allowed coaches updated for train type '{}': {} -> {}",
      typeCode, request.getCoachTypeCodes().size(), coachTypes.stream()
        .map(CoachTypeEntity::getTypeCode).toList());

    return coachTypes.stream().map(this::toAllowedCoachResponse).toList();
  }

  // ── Private helpers ─────────────────────────────────────────────────────────

  private TrainTypeEntity findByCode(String code) {
    return trainTypeRepository.findByTypeCode(code.toUpperCase())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_TYPE_NOT_FOUND",
        "Train type not found: " + code));
  }

  /**
   * Convert entity to response.
   * Derives isActive from the period table (active on today).
   * Includes effectiveFrom/effectiveTill from the current period.
   * Optionally includes full periods list (for admin/detail views).
   */
  private TrainTypeResponse toResponse(TrainTypeEntity e, boolean includePeriods, String message) {
    LocalDate today = LocalDate.now();
    boolean isActive = trainTypePeriodRepository.isActiveOnDate(e.getTypeId(), today);

    TrainTypeResponse.TrainTypeResponseBuilder builder = TrainTypeResponse.builder()
      .typeId(e.getTypeId())
      .typeCode(e.getTypeCode())
      .typeName(e.getTypeName())
      .description(e.getDescription())
      .typicalSpeedKmh(e.getTypicalSpeedKmh())
      .isSuperfast(e.getIsSuperfast())
      .isActive(isActive)
      .createdBy(e.getCreatedBy())
      .updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt())
      .updatedAt(e.getUpdatedAt())
      .message(message);

    // Populate effectiveFrom/effectiveTill from current active period
    Optional<TrainTypePeriodEntity> activePeriod =
      trainTypePeriodRepository.findActivePeriod(e.getTypeId(), today);
    activePeriod.ifPresent(p -> {
      builder.effectiveFrom(p.getEffectiveFrom());
      builder.effectiveTill(p.getEffectiveTill());
    });

    // Include full periods list for admin / detail views
    if (includePeriods) {
      List<PeriodResponse> periods = trainTypePeriodRepository.findAllByTypeId(e.getTypeId())
        .stream()
        .map(p -> toPeriodResponse(p, today))
        .toList();
      builder.periods(periods);
    }

    return builder.build();
  }

  /**
   * Convert a period entity to PeriodResponse with derived status.
   * ACTIVE   — covers today (effectiveFrom <= today AND (effectiveTill IS NULL OR effectiveTill >= today))
   * PAST     — effectiveTill < today
   * UPCOMING — effectiveFrom > today
   */
  private PeriodResponse toPeriodResponse(TrainTypePeriodEntity p, LocalDate today) {
    String status;
    if (p.getEffectiveFrom().isAfter(today)) {
      status = "UPCOMING";
    } else if (p.getEffectiveTill() != null && p.getEffectiveTill().isBefore(today)) {
      status = "PAST";
    } else {
      status = "ACTIVE";
    }

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

  private AllowedCoachResponse toAllowedCoachResponse(CoachTypeEntity ct) {
    return AllowedCoachResponse.builder()
      .coachTypeId(ct.getTypeId())
      .coachTypeCode(ct.getTypeCode())
      .coachTypeName(ct.getTypeName())
      .totalSeats(ct.getTotalSeats())
      .isAc(ct.getIsAc())
      .isActive(coachTypePeriodRepository.isActiveOnDate(ct.getTypeId(), LocalDate.now()))
      .build();
  }
}
