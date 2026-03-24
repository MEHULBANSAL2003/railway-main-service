package com.railway.main_service.dto.response.train;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainResponse {

  private Long    trainId;
  private String  trainNumber;
  private String  trainName;

  private String  trainTypeCode;
  private String  trainTypeName;
  private Boolean isSuperfast;    // derived from trainType — useful for display

  private String  zoneCode;
  private String  zoneName;

  private Boolean pantrycar;
  private Boolean isActive;       // derived from effectiveFrom/effectiveTill

  private LocalDate effectiveFrom;
  private LocalDate effectiveTill;
  private String    reason;

  private Long          createdBy;
  private Long          updatedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private String message;         // operation result message
}
