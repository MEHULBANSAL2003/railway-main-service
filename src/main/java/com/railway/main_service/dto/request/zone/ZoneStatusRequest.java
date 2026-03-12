package com.railway.main_service.dto.request.zone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class ZoneStatusRequest {

  @NotNull(message = "Effective from is required")
  private LocalDate effectiveFrom;

  // null = forever
  private LocalDate effectiveTill;

  @NotBlank(message = "Reason is required")
  @Size(max = 500, message = "Reason must be under 500 characters")
  private String reason;
}
