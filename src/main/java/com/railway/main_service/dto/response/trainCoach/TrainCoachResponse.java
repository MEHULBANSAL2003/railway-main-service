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

  // Coach type info (from CoachTypeEntity)
  private Long    coachTypeId;
  private String  coachTypeCode;
  private String  coachTypeName;
  private Boolean isAc;
  private Integer totalSeats;           // seats per coach — from CoachType

  // Config on this train
  private Integer coachCount;           // number of coaches (e.g. 6 → S1–S6)
  private Integer tatkalSeats;          // tatkal per coach
  private Integer racSeats;             // RAC per coach
  private Integer waitlistLimit;        // flat WL cap for this class on this train

  // Derived totals — computed in service, saved from repeated frontend math
  private Integer totalCoachSeats;      // coachCount × totalSeats
  private Integer totalTatkalSeats;     // coachCount × tatkalSeats
  private Integer totalRacSeats;        // coachCount × racSeats
  // waitlistLimit is already a flat total — no derived needed

  private Boolean isActive;
  private LocalDate effectiveFrom;
  private LocalDate effectiveTill;

  private Long    createdBy;
  private Long    updatedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String  message;
}
