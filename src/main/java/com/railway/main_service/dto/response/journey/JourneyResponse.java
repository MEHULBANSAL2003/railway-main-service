package com.railway.main_service.dto.response.journey;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class JourneyResponse {
  private Long      journeyId;
  private LocalDate journeyDate;
  private String    status;        // SCHEDULED / DEPARTED / COMPLETED / CANCELLED
  private boolean   chartPrepared;
  private boolean   cancelled;
  private String    cancelReason;
  private String    scheduleRunDays;
  private Long      scheduleId;
  private String    createdAt;
}
