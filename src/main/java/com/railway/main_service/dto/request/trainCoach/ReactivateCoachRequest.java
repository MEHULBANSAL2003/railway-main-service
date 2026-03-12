package com.railway.main_service.dto.request.trainCoach;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReactivateCoachRequest {

  @NotNull(message = "Effective from date is required.")
  @Future(message = "Effective from must be a future date.")
  private LocalDate effectiveFrom;

  @NotBlank(message = "Reason for reactivation is required.")
  @Size(max = 500)
  private String changeReason;
}
