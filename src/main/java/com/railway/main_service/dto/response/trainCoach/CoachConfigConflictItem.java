package com.railway.main_service.dto.response.trainCoach;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CoachConfigConflictItem {
  private Long   journeyId;
  private String journeyDate;   // dd/MM/yyyy
  private String conflictField; // "confirmedSeats" | "tatkalSeats" | "racSeats" | "waitlistLimit"
  private int    currentBooked;
  private int    newLimit;
}
