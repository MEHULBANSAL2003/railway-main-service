package com.railway.main_service.dto.request.coachType;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCoachTypeRequest {

  @Size(min = 2, max = 100, message = "Type name must be between 2 and 100 characters.")
  private String typeName;

  @Size(max = 255)
  private String description;

  @Min(value = 1) @Max(value = 200)
  private Integer totalSeats;

  private Boolean isAc;
}
