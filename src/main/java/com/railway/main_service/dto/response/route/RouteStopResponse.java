package com.railway.main_service.dto.response.route;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteStopResponse {

  private Long    stopId;
  private Long    routeId;
  private String  routeCode;

  // Station info
  private Long    stationId;
  private String  stationCode;
  private String  stationName;
  private String  stationType;

  private Integer stopNumber;
  private Integer kmFromSource;
  private Integer dayNumber;

  // Derived — distance from previous stop (useful for display)
  private Integer kmFromPrevious;

  private Long    createdBy;
  private Long    updatedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String  message;
}
