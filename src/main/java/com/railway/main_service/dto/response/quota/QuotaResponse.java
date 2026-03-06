package com.railway.main_service.dto.response.quota;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotaResponse {
  private Long quotaId;
  private String quotaCode;
  private String quotaName;
  private String description;
  private Boolean isActive;
  private Long createdBy;
  private Long updatedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String message;
}
