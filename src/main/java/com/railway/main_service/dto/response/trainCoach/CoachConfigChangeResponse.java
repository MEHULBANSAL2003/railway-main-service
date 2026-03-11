package com.railway.main_service.dto.response.trainCoach;

import lombok.*;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CoachConfigChangeResponse {
  private boolean success;
  private String  message;
  private int     affectedJourneys;   // how many inventory rows were updated

  // Non-null only when success=false (blocked)
  private List<CoachConfigConflictItem> conflicts;
}
