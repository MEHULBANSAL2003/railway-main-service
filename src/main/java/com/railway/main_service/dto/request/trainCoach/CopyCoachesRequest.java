package com.railway.main_service.dto.request.trainCoach;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CopyCoachesRequest {

  @NotBlank(message = "Target train number is required.")
  private String targetTrainNumber;
}
