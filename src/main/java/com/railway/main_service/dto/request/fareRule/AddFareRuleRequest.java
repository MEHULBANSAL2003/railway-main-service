package com.railway.main_service.dto.request.fareRule;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddFareRuleRequest {

  @NotBlank(message = "Train type code is required.")
  private String trainTypeCode;

  @NotBlank(message = "Coach type code is required.")
  private String coachTypeCode;

  @NotNull(message = "Base fare per km is required.")
  @DecimalMin(value = "0.0001", message = "Base fare must be greater than 0.")
  @Digits(integer = 4, fraction = 4)
  private BigDecimal baseFarePerKm;

  @NotNull(message = "Minimum fare is required.")
  @DecimalMin(value = "0.00", message = "Minimum fare cannot be negative.")
  @Digits(integer = 6, fraction = 2)
  private BigDecimal minFare;

  @NotNull(message = "Reservation charge is required.")
  @DecimalMin(value = "0.00", message = "Reservation charge cannot be negative.")
  @Digits(integer = 4, fraction = 2)
  private BigDecimal reservationCharge;

  @NotNull(message = "Superfast charge is required.")
  @DecimalMin(value = "0.00", message = "Superfast charge cannot be negative.")
  @Digits(integer = 4, fraction = 2)
  private BigDecimal superfastCharge;

  @NotNull(message = "GST percentage is required.")
  @DecimalMin(value = "0.00", message = "GST cannot be negative.")
  @DecimalMax(value = "100.00", message = "GST cannot exceed 100.")
  @Digits(integer = 2, fraction = 2)
  private BigDecimal gstPct;

  @NotNull(message = "Effective from date is required.")
  private LocalDate effectiveFrom;

  private LocalDate effectiveUntil;
}
