package com.railway.main_service.dto.response.trainSchedule;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainScheduleSummaryResponse {

  private TrainScheduleResponse       running;      // null if not running
  private List<TrainScheduleResponse> upcoming;     // sorted by startDate ASC
  private List<TrainScheduleResponse> past;         // sorted by startDate DESC
  private List<TrainScheduleResponse> deactivated;  // sorted by startDate DESC
}
