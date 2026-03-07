package com.railway.main_service.dto.request.train;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AddTrainRequest {

  @NotBlank(message = "Train number is required.")
  @Pattern(
    regexp  = "^[0-9]{5}$",
    message = "Train number must be exactly 5 digits."
  )
  private String trainNumber;

  @NotBlank(message = "Train name is required.")
  @Size(min = 3, max = 150, message = "Train name must be between 3 and 150 characters.")
  private String trainName;

  @NotBlank(message = "Train type code is required.")
  private String trainTypeCode;

  @NotBlank(message = "Zone code is required.")
  private String zoneCode;

  @NotNull(message = "Pantry car field is required.")
  private Boolean pantrycar;
}
