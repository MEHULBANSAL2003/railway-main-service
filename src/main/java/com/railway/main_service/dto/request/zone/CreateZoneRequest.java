package com.railway.main_service.dto.request.zone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class CreateZoneRequest {

  @NotBlank(message = "Zone name is required")
  @Size(max = 100, message = "Zone name must be under 100 characters")
  private String zoneName;

  @NotBlank(message = "Zone code is required")
  @Size(max = 10, message = "Zone code must be under 10 characters")
  private String zoneCode;

  @NotNull(message = "Effective from is required")
  private LocalDate effectiveFrom;
}
