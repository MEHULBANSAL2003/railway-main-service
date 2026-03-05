package com.railway.main_service.dto.response.fareRule;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FareRuleResponse {
  private Long ruleId;
  private String trainTypeCode;
  private String trainTypeName;
  private Boolean isSuperfast;
  private String coachTypeCode;
  private String coachTypeName;
  private Boolean isAc;
  private BigDecimal baseFarePerKm;
  private BigDecimal minFare;
  private BigDecimal reservationCharge;
  private BigDecimal superfastCharge;
  private BigDecimal gstPct;
  private LocalDate effectiveFrom;
  private LocalDate effectiveUntil;
  private Boolean isActive;
  private Boolean isCurrent;
  private Long createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String message;
}
