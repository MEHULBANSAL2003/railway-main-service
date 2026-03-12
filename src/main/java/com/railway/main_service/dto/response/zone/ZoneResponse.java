package com.railway.main_service.dto.response.zone;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ZoneResponse {
  private Long zoneId;
  private String zoneName;
  private String zoneCode;
  private String reason;
  private Long createdBy;
  private LocalDateTime createdAt;
  private boolean isCurrentlyActive;
}
