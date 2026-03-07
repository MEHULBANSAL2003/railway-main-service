package com.railway.main_service.dto.request.trainCoach;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddTrainCoachRequest {

  @NotBlank(message = "Coach type code is required.")
  private String coachTypeCode;

  @NotNull(message = "Coach count is required.")
  @Min(value = 1,  message = "Coach count must be at least 1.")
  @Max(value = 30, message = "Coach count cannot exceed 30.")
  private Integer coachCount;

  @NotNull(message = "Tatkal seats is required.")
  @Min(value = 0, message = "Tatkal seats cannot be negative.")
  private Integer tatkalSeats;

  // Per coach — validated against coachType.totalSeats in service
  @NotNull(message = "RAC seats is required.")
  @Min(value = 0, message = "RAC seats cannot be negative.")
  private Integer racSeats;

  // Flat total WL pool for this class on this train
  @NotNull(message = "Waitlist limit is required.")
  @Min(value = 0,    message = "Waitlist limit cannot be negative.")
  @Max(value = 1000, message = "Waitlist limit cannot exceed 1000.")
  private Integer waitlistLimit;
}
