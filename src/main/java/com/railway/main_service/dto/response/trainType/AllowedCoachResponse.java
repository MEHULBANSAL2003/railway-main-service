package com.railway.main_service.dto.response.trainType;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AllowedCoachResponse {
  private Long    coachTypeId;
  private String  coachTypeCode;
  private String  coachTypeName;
  private Integer totalSeats;
  private Boolean isAc;
  private Boolean isActive;   // coach type's own active status
}
