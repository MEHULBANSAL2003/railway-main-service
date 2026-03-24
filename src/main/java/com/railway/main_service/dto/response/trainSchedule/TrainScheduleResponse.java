package com.railway.main_service.dto.response.trainSchedule;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainScheduleResponse {

  private Long         scheduleId;
  private String       trainNumber;
  private List<String> runDays;       // ["MON","WED","FRI"] in week order
  private LocalDate    startDate;
  private LocalDate    endDate;       // null = indefinite
  private Boolean      isActive;
  private LocalDate    effectiveFrom;
  private LocalDate    effectiveTill;
  private String       reason;
  private String       status;        // RUNNING | UPCOMING | PAST | DEACTIVATED
  private List<String> addedDays;     // diff vs previous (history only)
  private List<String> removedDays;   // diff vs previous (history only)
  private Long         createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String       message;
}
