package com.railway.main_service.dto.response.trainType;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AllowedCoachResponse {
  private Long    coachTypeId;
  private String  coachTypeCode;
  private String  coachTypeName;
  private Integer totalSeats;
  private Boolean isAc;
  private Boolean isActive;   // derived from effectiveFrom/effectiveTill
  private LocalDate effectiveFrom;
  private LocalDate effectiveTill;
  private String reason;
}
