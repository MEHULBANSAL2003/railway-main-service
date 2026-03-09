package com.railway.main_service.dto.response.trainCoach;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainCopyCoachesResponse {

  private String               sourceTrainNumber;
  private String               targetTrainNumber;
  private int                  copiedCount;        // how many coach types copied
  private List<TrainCoachResponse> coaches;        // the newly created coaches
  private String               message;
}
