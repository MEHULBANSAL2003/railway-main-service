package com.railway.main_service.dto.request.route;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddRouteStopRequest {

  @NotBlank(message = "Station code is required.")
  private String stationCode;

  // If null — auto-appended as last stop (stopNumber = max + 1)
  // If provided — inserted at this position, existing stops shifted down
  @Min(value = 1, message = "Stop number must be at least 1.")
  private Integer stopNumber;

  @NotNull(message = "KM from source is required.")
  @Min(value = 0, message = "KM from source cannot be negative.")
  private Integer kmFromSource;

  @NotNull(message = "Day number is required.")
  @Min(value = 1, message = "Day number must be at least 1.")
  @Max(value = 7, message = "Day number cannot exceed 7.")
  private Integer dayNumber;
}
