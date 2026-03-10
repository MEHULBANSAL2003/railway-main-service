package com.railway.main_service.dto.response.inventory;

import lombok.*;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class JourneyInventoryResponse {
  private Long   journeyId;
  private String journeyDate;
  private String status;
  private List<CoachInventoryResponse> coaches;
}
