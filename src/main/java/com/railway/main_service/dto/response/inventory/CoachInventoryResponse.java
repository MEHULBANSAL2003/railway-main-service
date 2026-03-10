package com.railway.main_service.dto.response.inventory;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CoachInventoryResponse {
  private Long   coachId;
  private String coachTypeCode;
  private String coachTypeName;
  private boolean isAc;
  private int    coachCount;
  private QuotaInventoryResponse general;  // always present
  private QuotaInventoryResponse tatkal;   // null if no tatkal configured
}
