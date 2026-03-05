package com.railway.main_service.dto.response.station;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.railway.main_service.enums.StationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddNewStationResponse {

  private Long stationId;
  private String stationCode;
  private String stationName;
  private String cityName;
  private String stateName;
  private String zoneName;
  private Boolean isActive;
  private StationType stationType;
  private int numPlatforms;
  private Long createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long updatedBy;
  private String message;

}
