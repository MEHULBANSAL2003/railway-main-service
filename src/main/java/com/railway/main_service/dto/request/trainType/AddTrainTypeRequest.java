package com.railway.main_service.dto.request.trainType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddTrainTypeRequest {
  @NotBlank(message = "Type code is required.")
  @Size(min = 2, max = 20, message = "Type code must be between 2 and 20 characters.")
  private String typeCode;

  @NotBlank(message = "Type name is required.")
  @Size(min = 2, max = 100, message = "Type name must be between 2 and 100 characters.")
  private String typeName;

  @Size(max = 255, message = "Description must be under 255 characters.")
  private String description;

  @Min(value = 1, message = "Speed must be at least 1 kmh.")
  @Max(value = 600, message = "Speed must be under 600 kmh.")
  private Integer typicalSpeedKmh;

  @NotNull(message = "isSuperfast is required.")
  private Boolean isSuperfast;
}
