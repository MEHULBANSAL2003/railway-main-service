package com.railway.main_service.dto.request.station;

import com.railway.main_service.enums.StationType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddNewStationRequest {

  @NotBlank(message = "Station Code is required")
  @Size(min = 2, max = 5)
  @Pattern(regexp = "^[A-Z]{2,5}$", message = "Station Code must be 2-5 uppercase letters")
  private String stationCode;

  @NotBlank(message = "Station Name is required")
  @Size(min = 3, max = 100)
  private String stationName;

  @NotNull(message = "State Id is required")
  private Long stateId;

  @NotNull(message = "City Id is required")
  private Long cityId;


  @NotNull(message = "Zone Id is required")
  private Long zoneId;

  @NotNull(message = "Station Type is required")
  private StationType stationType;

  @Min(value = 1, message = "Number of platforms must be at least 1")
  @Max(value = 25, message = "Number of platforms cannot exceed 25")
  private Integer numPlatforms;

  private Double latitude;
  private Double longitude;
}
