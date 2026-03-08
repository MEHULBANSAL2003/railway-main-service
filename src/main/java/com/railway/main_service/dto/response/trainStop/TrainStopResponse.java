package com.railway.main_service.dto.response.trainStop;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainStopResponse {

  private Long    stopId;
  private String  trainNumber;

  private Long    stationId;
  private String  stationCode;
  private String  stationName;
  private String  stationType;   // JUNCTION, TERMINAL etc.

  private Integer stopNumber;
  private Integer kmFromSource;
  private Integer kmFromPrevious; // derived — distance from previous stop

  private String  arrivalTime;    // "HH:mm"
  private String  departureTime;  // "HH:mm"
  private Integer dayNumber;

  private Long          createdBy;
  private Long          updatedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String        message;
}
