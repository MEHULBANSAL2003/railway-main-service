package com.railway.main_service.dto.request.station;

import com.railway.main_service.enums.StationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UpdateStationRequest {

  private String stationName;


  private Long cityId;

  private Long stateId;

  private Long zoneId;

  private StationType stationType;


  @Min(1)
  @Max(25)
  private Integer numPlatforms;

}
