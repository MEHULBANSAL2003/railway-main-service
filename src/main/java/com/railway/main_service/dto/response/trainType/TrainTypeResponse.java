package com.railway.main_service.dto.response.trainType;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainTypeResponse {

  private Long typeId;
  private String typeCode;
  private String typeName;
  private String description;
  private Integer typicalSpeedKmh;
  private Boolean isSuperfast;
  private Boolean isActive;
  private Long createdBy;
  private Long updatedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String message;
}
