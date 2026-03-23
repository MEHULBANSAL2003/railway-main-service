package com.railway.main_service.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeactivationResponse {
  private String entityType;
  private String entityCode;
  private String entityName;
  private String action;          // DEACTIVATED, ACTIVATED
  private PeriodResponse period;  // The period that was created/modified

  // Cascade summary
  private int affectedFareRules;
  private int affectedCoaches;
  private int affectedSchedules;
  private int cancelledJourneys;
  private int deletedInventoryRows;

  // Warnings
  private List<String> warnings;  // e.g. "Journey on 2026-04-01 has 45 booked passengers"

  private String message;
}
