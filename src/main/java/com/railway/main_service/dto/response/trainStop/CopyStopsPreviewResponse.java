package com.railway.main_service.dto.response.trainStop;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CopyStopsPreviewResponse {

  private String sourceTrainNumber;
  private String targetTrainNumber;
  private int    stopCount;

  // Pre-filled stops in reversed order — times are null, user fills them
  private List<CopyStopRow> stops;

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class CopyStopRow {
    private int    stopNumber;
    private String stationCode;
    private String stationName;
    private String stationType;
    private int    kmFromSource;   // recalculated from reversed source
    private int    dayNumber;
    private boolean isFirst;       // true → no arrival field
    private boolean isLast;        // true → no departure field
    // times left null for user to fill
    private String arrivalTime;
    private String departureTime;
  }
}
