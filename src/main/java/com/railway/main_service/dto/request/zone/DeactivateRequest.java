package com.railway.main_service.dto.request.zone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DeactivateRequest {

  @NotNull(message = "Effective till is required")
  private LocalDate effectiveTill;

  @NotBlank(message = "Reason is required")
  private String reason;
}
