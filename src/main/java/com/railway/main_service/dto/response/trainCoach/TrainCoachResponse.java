package com.railway.main_service.dto.response.trainCoach;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainCoachResponse {

  private Long    coachId;

  // Train context
  private Long    trainId;
  private String  trainNumber;
  private String  trainName;

  // Coach type info
  private Long    coachTypeId;
  private String  coachTypeCode;
  private String  coachTypeName;
  private Boolean isAc;
  private Integer totalSeats;           // seats per coach (from CoachType)

  // Config
  private Integer coachCount;
  private Integer tatkalSeats;
  private Integer racSeats;
  private Integer waitlistLimit;

  // Derived totals
  private Integer totalCoachSeats;
  private Integer totalTatkalSeats;
  private Integer totalRacSeats;

  // Effective date range
  private LocalDate effectiveFrom;
  private LocalDate effectiveTill;      // null = currently open / active
  private String    reason;

  // Status
  private Boolean isActive;

  // Audit
  private Long          createdBy;
  private Long          updatedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Response message (only on write operations)
  private String message;
}
