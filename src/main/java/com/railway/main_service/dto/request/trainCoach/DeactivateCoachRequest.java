package com.railway.main_service.dto.request.trainCoach;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeactivateCoachRequest {

  @NotNull(message = "Effective from date is required.")
  @Future(message = "Effective from must be a future date.")
  private LocalDate effectiveFrom;

  // Optional — if set, coach is only suspended between from→to, then reactivates
  private LocalDate effectiveTill;

  @NotBlank(message = "Reason for deactivation is required.")
  @Size(max = 500)
  private String reason;
}
