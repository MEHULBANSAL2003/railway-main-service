package com.railway.main_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActivateRequest {

  @NotNull(message = "fromDate is required")
  private LocalDate fromDate;

  // null = active forever (no planned end)
  private LocalDate tillDate;

  private String reason;
}
