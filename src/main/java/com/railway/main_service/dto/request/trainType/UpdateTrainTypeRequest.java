package com.railway.main_service.dto.request.trainType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTrainTypeRequest {
  @Size(min = 2, max = 100, message = "Type name must be between 2 and 100 characters.")
  private String typeName;

  @Size(max = 255)
  private String description;

  @Min(1) @Max(600)
  private Integer typicalSpeedKmh;

  private Boolean isSuperfast;
}
