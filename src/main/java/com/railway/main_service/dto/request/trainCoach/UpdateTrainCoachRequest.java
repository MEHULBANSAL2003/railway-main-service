package com.railway.main_service.dto.request.trainCoach;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateTrainCoachRequest {

  // coachType is immutable — only operational fields can change
  @Min(value = 1,  message = "Coach count must be at least 1.")
  @Max(value = 10, message = "Coach count cannot exceed 30.")
  private Integer coachCount;

  @Min(value = 0, message = "Tatkal seats cannot be negative.")
  private Integer tatkalSeats;

  @Min(value = 0, message = "RAC seats cannot be negative.")
  private Integer racSeats;

  @Min(value = 0,    message = "Waitlist limit cannot be negative.")
  @Max(value = 100, message = "Waitlist limit cannot exceed 1000.")
  private Integer waitlistLimit;
}
