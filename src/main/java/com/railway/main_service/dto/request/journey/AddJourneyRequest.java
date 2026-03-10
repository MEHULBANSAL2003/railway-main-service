package com.railway.main_service.dto.request.journey;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter
public class AddJourneyRequest {

  @NotNull(message = "Journey date is required")
  @Future(message = "Journey date must be in the future")
  private LocalDate journeyDate;
}
