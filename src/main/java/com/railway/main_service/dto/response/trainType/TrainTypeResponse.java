package com.railway.main_service.dto.response.trainType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.railway.main_service.dto.response.PeriodResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

  private List<PeriodResponse> periods;
  private LocalDate effectiveFrom;
  private LocalDate effectiveTill;

  private Long createdBy;
  private Long updatedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String message;
}
