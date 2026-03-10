package com.railway.main_service.dto.response.inventory;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QuotaInventoryResponse {
  private Long   inventoryId;
  private String quotaType;          // GENERAL | TATKAL

  // Confirmed
  private int     totalSeats;
  private int     bookedConfirmed;
  private int     availableConfirmed;

  // RAC — null for TATKAL
  private Integer totalRac;
  private Integer bookedRac;
  private Integer availableRac;

  // Waitlist — null for TATKAL
  private Integer waitlistLimit;
  private Integer bookedWaitlist;
  private Integer availableWaitlist;
}
