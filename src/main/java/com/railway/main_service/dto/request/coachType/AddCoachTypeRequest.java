package com.railway.main_service.dto.request.coachType;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddCoachTypeRequest {

  @NotBlank(message = "Type code is required.")
  @Size(min = 1, max = 10, message = "Type code must be between 1 and 10 characters.")
  private String typeCode;

  @NotBlank(message = "Type name is required.")
  @Size(min = 2, max = 100, message = "Type name must be between 2 and 100 characters.")
  private String typeName;

  @Size(max = 255, message = "Description must be under 255 characters.")
  private String description;

  @NotNull(message = "Total seats is required.")
  @Min(value = 1, message = "Total seats must be at least 1.")
  @Max(value = 200, message = "Total seats must be under 200.")
  private Integer totalSeats;

  @NotNull(message = "isAc is required.")
  private Boolean isAc;
}
