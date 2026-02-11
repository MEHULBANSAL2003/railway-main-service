package com.railway.main_service.dto.response.station;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StationResponse {
  private Long stationId;
  private String stationCode;
  private String stationName;
  private String city;
  private String state;
  private String zone;
  private int numPlatforms;
  private boolean isJunction;
  private boolean canUpdatedByCurrentAdmin;
  private boolean canDeletedByCurrentAdmin;
  private LocalDateTime createdAt;
}
