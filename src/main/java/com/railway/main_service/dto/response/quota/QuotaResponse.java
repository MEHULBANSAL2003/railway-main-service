package com.railway.main_service.dto.response.quota;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.railway.main_service.dto.response.PeriodResponse;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotaResponse {
  private Long quotaId;
  private String quotaCode;
  private String quotaName;
  private String description;
  private Boolean isActive;

  private List<PeriodResponse> periods;
  private LocalDate effectiveFrom;
  private LocalDate effectiveTill;

  private Long createdBy;
  private Long updatedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String message;
}
