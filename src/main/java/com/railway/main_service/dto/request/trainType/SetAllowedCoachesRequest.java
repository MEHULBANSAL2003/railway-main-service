package com.railway.main_service.dto.request.trainType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class SetAllowedCoachesRequest {

  // Full replace — send the complete list of allowed coach type codes
  // Empty list = clear all allowed coaches for this train type
  @NotNull(message = "Coach type codes list is required.")
  private List<String> coachTypeCodes;
}
