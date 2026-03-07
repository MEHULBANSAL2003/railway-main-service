package com.railway.main_service.dto.response.train;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReturnTrainResponse {

  // The suggested return train number (original +1 or -1 based on odd/even rule)
  private String returnTrainNumber;

  // Does this return train already exist in the system?
  private Boolean exists;

  // If it exists — full train details so frontend can display them
  // Null if train does not exist yet
  private TrainResponse existingTrain;

  // Human-readable message for the UI prompt
  private String message;
}
