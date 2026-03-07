package com.railway.main_service.dto.response.trainCoach;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CoachTypeDropdownResponse {
  private Long    typeId;
  private String  typeCode;
  private String  typeName;
  private Integer totalSeats;
  private Boolean isAc;
}
