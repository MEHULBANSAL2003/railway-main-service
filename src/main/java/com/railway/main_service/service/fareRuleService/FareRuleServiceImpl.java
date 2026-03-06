package com.railway.main_service.service.fareRuleService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.fareRule.AddFareRuleRequest;
import com.railway.main_service.dto.response.fareRule.FareRuleResponse;
import com.railway.main_service.entity.CoachTypeEntity;
import com.railway.main_service.entity.FareRuleEntity;
import com.railway.main_service.entity.QuotaEntity;
import com.railway.main_service.entity.TrainTypeEntity;
import com.railway.main_service.repository.CoachTypeRepository;
import com.railway.main_service.repository.FareRuleRepository;
import com.railway.main_service.repository.QuotaRepository;
import com.railway.main_service.repository.TrainTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Loggable
@Slf4j
@RequiredArgsConstructor
public class FareRuleServiceImpl implements FareRuleService {

  private final FareRuleRepository   fareRuleRepository;
  private final TrainTypeRepository  trainTypeRepository;
  private final CoachTypeRepository  coachTypeRepository;
  private final QuotaRepository quotaRepository;

  private static final Map<String, int[]> TATKAL_BOUNDS = Map.of(
    "SL",  new int[]{100, 200},
    "3A",  new int[]{300, 400},
    "2A",  new int[]{400, 500},
    "1A",  new int[]{500, 600},
    "CC",  new int[]{150, 250},
    "EC",  new int[]{300, 400},
    "3E",  new int[]{300, 400},
    "FC",  new int[]{200, 300}
  );

  @Override
  @Transactional
  public FareRuleResponse addFareRule(AddFareRuleRequest request) {

    String trainCode = request.getTrainTypeCode().trim().toUpperCase();
    String coachCode = request.getCoachTypeCode().trim().toUpperCase();
    String quotaCode = request.getQuotaCode().trim().toUpperCase();
    BigDecimal tatkalCharge = request.getTatkalCharge();

    QuotaEntity quota = quotaRepository.findByQuotaCode(quotaCode)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "QUOTA_NOT_FOUND",
        "Quota not found: " + quotaCode));
    if (!quota.getIsActive()) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "QUOTA_INACTIVE",
        "Quota '" + quotaCode + "' is inactive.");
    }

    // Validate train type exists and is active
    TrainTypeEntity trainType = trainTypeRepository.findByTypeCode(trainCode)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_TYPE_NOT_FOUND",
        "Train type not found: " + trainCode));

    if (!trainType.getIsActive()) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "TRAIN_TYPE_INACTIVE",
        "Train type '" + trainCode + "' is inactive. Activate it before adding fare rules.");
    }

    // Validate coach type exists and is active
    CoachTypeEntity coachType = coachTypeRepository.findByTypeCode(coachCode)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "COACH_TYPE_NOT_FOUND",
        "Coach type not found: " + coachCode));

    if (!coachType.getIsActive()) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "COACH_TYPE_INACTIVE",
        "Coach type '" + coachCode + "' is inactive. Activate it before adding fare rules.");
    }

    // Validate effectiveUntil is after effectiveFrom
    if (request.getEffectiveUntil() != null &&
      !request.getEffectiveUntil().isAfter(request.getEffectiveFrom())) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
        "Effective until date must be after effective from date.");
    }

    // Check duplicate combo + date
    if (fareRuleRepository.existsByTrainType_TypeCodeAndCoachType_TypeCodeAndQuota_QuotaCodeAndEffectiveFrom(
      trainCode, coachCode, quotaCode, request.getEffectiveFrom())) {
      throw new BaseException(HttpStatus.CONFLICT, "FARE_RULE_EXISTS",
        "A fare rule for " + trainCode + " + " + coachCode +
          " effective from " + request.getEffectiveFrom() + " already exists.");
    }

    if ("TATKAL".equals(quotaCode)) {
      int[] bounds = TATKAL_BOUNDS.get(coachCode);
      if (bounds == null) {
        throw new BaseException(HttpStatus.BAD_REQUEST, "TATKAL_NOT_SUPPORTED",
          "Tatkal quota is not supported for coach type: " + coachCode);
      }
      if (tatkalCharge.compareTo(BigDecimal.valueOf(bounds[0])) < 0 ||
        tatkalCharge.compareTo(BigDecimal.valueOf(bounds[1])) > 0) {
        throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_TATKAL_CHARGE",
          "Tatkal charge for " + coachCode + " must be between ₹" + bounds[0] +
            " and ₹" + bounds[1] + ". Received: ₹" + tatkalCharge);
      }
    } else {
      // Non-tatkal quota must have zero tatkal charge
      if (tatkalCharge.compareTo(BigDecimal.ZERO) != 0) {
        throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_TATKAL_CHARGE",
          "Tatkal charge must be 0 for non-Tatkal quotas.");
      }
    }


    // Auto-close previous open rule for this combo
    fareRuleRepository.findOpenRule(trainCode, coachCode, quotaCode).ifPresent(existing -> {
      existing.setEffectiveUntil(request.getEffectiveFrom().minusDays(1));
      existing.setUpdatedBy(SecurityUtils.getCurrentAdminId());
      fareRuleRepository.save(existing);
    });

    FareRuleEntity entity = FareRuleEntity.builder()
      .trainType(trainType)
      .coachType(coachType)
      .quota(quota)
      .baseFarePerKm(request.getBaseFarePerKm())
      .tatkalCharge(tatkalCharge)
      .minFare(request.getMinFare())
      .reservationCharge(request.getReservationCharge())
      .superfastCharge(request.getSuperfastCharge())
      .gstPct(request.getGstPct())
      .effectiveFrom(request.getEffectiveFrom())
      .effectiveUntil(request.getEffectiveUntil())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    FareRuleEntity saved = fareRuleRepository.save(entity);
    return toResponse(saved, "Fare rule added successfully.");
  }

  @Override
  @Transactional
  public FareRuleResponse toggleStatus(Long ruleId, boolean isActive) {
    FareRuleEntity entity = fareRuleRepository.findById(ruleId)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "FARE_RULE_NOT_FOUND",
        "Fare rule not found with id: " + ruleId));

    if (entity.getIsActive().equals(isActive))
      return toResponse(entity, null);

    entity.setIsActive(isActive);
    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    return toResponse(fareRuleRepository.save(entity),
      "Fare rule " + (isActive ? "activated" : "deactivated") + " successfully.");
  }

  @Override
  public List<FareRuleResponse> getAllForAdmin(String trainTypeCode, String coachTypeCode,String quotaCode) {
    String tc = (trainTypeCode != null && !trainTypeCode.isBlank()) ? trainTypeCode.toUpperCase() : null;
    String cc = (coachTypeCode != null && !coachTypeCode.isBlank()) ? coachTypeCode.toUpperCase() : null;
    String qc = (quotaCode != null && !quotaCode.isBlank()) ? quotaCode.toUpperCase() : null;
    return fareRuleRepository.findAllForAdmin(tc, cc, qc)
      .stream().map(e -> toResponse(e, null)).toList();
  }

  @Override
  public List<FareRuleResponse> getComboHistory(String trainTypeCode, String coachTypeCode) {
    return fareRuleRepository.findAllByCombo(
        trainTypeCode.toUpperCase(), coachTypeCode.toUpperCase())
      .stream().map(e -> toResponse(e, null)).toList();
  }

  @Override
  public FareRuleResponse getCurrentRule(String trainTypeCode, String coachTypeCode, LocalDate date) {
    return fareRuleRepository.findCurrentRule(
        trainTypeCode.toUpperCase(), coachTypeCode.toUpperCase(), date)
      .map(e -> toResponse(e, null))
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "FARE_RULE_NOT_FOUND",
        "No active fare rule found for " + trainTypeCode + " + " + coachTypeCode + " on " + date));
  }

  // ── Mapper ───────────────────────────────────────────────
  private FareRuleResponse toResponse(FareRuleEntity e, String message) {
    boolean isCurrent = e.getIsActive()
      && !LocalDate.now().isBefore(e.getEffectiveFrom())
      && (e.getEffectiveUntil() == null || !LocalDate.now().isAfter(e.getEffectiveUntil()));

    return FareRuleResponse.builder()
      .ruleId(e.getRuleId())
      .trainTypeCode(e.getTrainType().getTypeCode())
      .trainTypeName(e.getTrainType().getTypeName())
      .quotaCode(e.getQuota().getQuotaCode())
      .quotaName(e.getQuota().getQuotaName())
      .isSuperfast(e.getTrainType().getIsSuperfast())
      .coachTypeCode(e.getCoachType().getTypeCode())
      .coachTypeName(e.getCoachType().getTypeName())
      .isAc(e.getCoachType().getIsAc())
      .baseFarePerKm(e.getBaseFarePerKm())
      .tatkalCharge(e.getTatkalCharge())
      .minFare(e.getMinFare())
      .reservationCharge(e.getReservationCharge())
      .superfastCharge(e.getSuperfastCharge())
      .gstPct(e.getGstPct())
      .effectiveFrom(e.getEffectiveFrom())
      .effectiveUntil(e.getEffectiveUntil())
      .isActive(e.getIsActive())
      .isCurrent(isCurrent)
      .createdBy(e.getCreatedBy())
      .createdAt(e.getCreatedAt())
      .updatedAt(e.getUpdatedAt())
      .message(message)
      .build();
  }
}
