package com.railway.main_service.dto.response.route;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteResponse {

  private Long    routeId;
  private String  routeCode;
  private String  routeName;
  private Integer totalKm;

  // Source — first stop
  private Long    sourceStationId;
  private String  sourceStationCode;
  private String  sourceStationName;

  // Destination — last stop
  private Long    destinationStationId;
  private String  destinationStationCode;
  private String  destinationStationName;

  private Integer totalStops;         // count of stops
  private Integer activeTrains;       // trains currently using this route
  private Boolean isActive;

  private Long    createdBy;
  private Long    updatedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String  message;

  // Populated when fetching route detail with stops
  private List<RouteStopResponse> stops;
}
