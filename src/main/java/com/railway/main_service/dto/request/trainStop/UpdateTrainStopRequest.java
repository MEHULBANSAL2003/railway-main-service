package com.railway.main_service.dto.request.trainStop;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateTrainStopRequest {

  // stationCode is IMMUTABLE — delete and re-add to change station
  // stopNumber  is IMMUTABLE — position cannot change via update

  @Min(value = 0, message = "KM from source cannot be negative.")
  private Integer kmFromSource;

  private String arrivalTime;   // null = clear it
  private String departureTime; // null = clear it

  @Min(value = 1, message = "Day number must be at least 1.")
  @Max(value = 7, message = "Day number cannot exceed 7.")
  private Integer dayNumber;
}
