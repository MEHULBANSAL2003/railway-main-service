package com.railway.main_service.dto.request.train;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateTrainRequest {

  // trainNumber is immutable — not updatable after creation
  // trainTypeCode cannot be changed — changing type would invalidate all fare rules.
  // If train type genuinely changes, deactivate old train and create a new one.

  @Size(min = 3, max = 150, message = "Train name must be between 3 and 150 characters.")
  private String trainName;

  // trainTypeCode is not updatable — changing type would invalidate all fare rules
  // If train type genuinely changes, deactivate old train and create new one

  private String zoneCode;    // zone transfer is allowed

  private Boolean pantrycar;  // pantry can be added/removed from a train
}
