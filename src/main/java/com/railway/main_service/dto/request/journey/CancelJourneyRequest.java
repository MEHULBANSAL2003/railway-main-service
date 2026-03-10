package com.railway.main_service.dto.request.journey;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CancelJourneyRequest {

  @NotBlank(message = "Cancellation reason is required")
  private String reason;
}
