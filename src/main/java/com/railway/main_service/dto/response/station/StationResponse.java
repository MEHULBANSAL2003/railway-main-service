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
public class StationResponse {
  private Long stationId;
  private String stationCode;
  private String stationName;

  // City info
  private Long cityId;
  private String cityName;

  // State info (via city -> state)
  private Long stateId;
  private String stateName;
  private String stateCode;

  // Zone info
  private Long zoneId;
  private String zoneName;
  private String zoneCode;

  private StationType stationType;
  private Integer numPlatforms;
  private Double latitude;
  private Double longitude;
  private Boolean isActive;

  // Admin permissions
  private boolean canUpdatedByCurrentAdmin;
  private boolean canDeletedByCurrentAdmin;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
