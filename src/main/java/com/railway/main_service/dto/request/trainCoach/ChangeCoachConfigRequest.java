package com.railway.main_service.dto.request.trainCoach;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChangeCoachConfigRequest {

  @NotNull(message = "Coach count is required.")
  @Min(value = 1,  message = "Coach count must be at least 1.")
  @Max(value = 30, message = "Coach count cannot exceed 30.")
  private Integer coachCount;

  @NotNull(message = "Tatkal seats is required.")
  @Min(value = 0, message = "Tatkal seats cannot be negative.")
  private Integer tatkalSeats;

  @NotNull(message = "RAC seats is required.")
  @Min(value = 0, message = "RAC seats cannot be negative.")
  private Integer racSeats;

  @NotNull(message = "Waitlist limit is required.")
  @Min(value = 0,    message = "Waitlist limit cannot be negative.")
  @Max(value = 1000, message = "Waitlist limit cannot exceed 1000.")
  private Integer waitlistLimit;

  @NotNull(message = "Effective from date is required.")
  @Future(message = "Effective from must be a future date.")
  private LocalDate effectiveFrom;

  // Optional end date
  private LocalDate effectiveTill;

  @NotBlank(message = "Reason for change is required.")
  @Size(max = 500)
  private String reason;
}
