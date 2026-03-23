package com.railway.main_service.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PeriodResponse {
  private Long periodId;
  private LocalDate effectiveFrom;
  private LocalDate effectiveTill;
  private String status;       // ACTIVE, PAST, UPCOMING, INACTIVE_GAP
  private String reason;
  private Long createdBy;
  private LocalDateTime createdAt;
}
