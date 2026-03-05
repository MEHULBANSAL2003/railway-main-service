package com.railway.main_service.dto.response.coachType;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CoachTypeResponse {
  private Long typeId;
  private String typeCode;
  private String typeName;
  private String description;
  private Integer totalSeats;
  private Boolean isAc;
  private Boolean isActive;
  private Long createdBy;
  private Long updatedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String message;
}
