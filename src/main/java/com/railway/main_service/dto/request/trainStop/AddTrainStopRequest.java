package com.railway.main_service.dto.request.trainStop;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddTrainStopRequest {

  @NotBlank(message = "Station code is required.")
  private String stationCode;

  // If null → auto-appended as last stop
  // If provided → inserted at this position, existing stops shifted
  @Min(value = 1, message = "Stop number must be at least 1.")
  private Integer stopNumber;

  @NotNull(message = "KM from source is required.")
  @Min(value = 0, message = "KM from source cannot be negative.")
  private Integer kmFromSource;

  // null = no arrival (valid for first stop only)
  private String arrivalTime;   // "HH:mm"

  // null = no departure (valid for last stop only)
  private String departureTime; // "HH:mm"

  @NotNull(message = "Day number is required.")
  @Min(value = 1, message = "Day number must be at least 1.")
  @Max(value = 7, message = "Day number cannot exceed 7.")
  private Integer dayNumber;
}
