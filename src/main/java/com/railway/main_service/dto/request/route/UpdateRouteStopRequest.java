package com.railway.main_service.dto.request.route;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateRouteStopRequest {

  // stationCode is immutable — change by deleting and re-adding
  @Min(value = 0, message = "KM from source cannot be negative.")
  private Integer kmFromSource;

  @Min(value = 1, message = "Day number must be at least 1.")
  @Max(value = 7, message = "Day number cannot exceed 7.")
  private Integer dayNumber;
}
