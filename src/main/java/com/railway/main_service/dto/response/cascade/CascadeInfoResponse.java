package com.railway.main_service.dto.response.cascade;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CascadeInfoResponse {
  private String entityType;      // COACH_TYPE / TRAIN_TYPE / QUOTA
  private String entityCode;
  private String entityName;
  private boolean currentlyActive;
  private int activeFareRulesCount;
  private String message;
}
