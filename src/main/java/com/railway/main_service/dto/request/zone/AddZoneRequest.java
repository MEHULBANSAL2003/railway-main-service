package com.railway.main_service.dto.request.zone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddZoneRequest {

  @NotBlank(message = "Zone code is required")
  @Size(min = 2, max = 10, message = "Zone code must be between 2 and 10 characters")
  @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Zone code must be alphanumeric without spaces")
  private String zoneCode;

  @NotBlank(message = "Zone name is required")
  @Size(min = 3, max = 100, message = "Zone name must be between 3 and 100 characters")
  private String zoneName;
}
