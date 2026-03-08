package com.railway.main_service.dto.request.trainSchedule;

import com.railway.main_service.enums.RunDay;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class AddTrainScheduleRequest {

  @NotNull(message = "Run days are required.")
  @Size(min = 1, message = "At least one day must be selected.")
  private Set<RunDay> runDays;  // e.g. [MON, WED, FRI]

  @NotNull(message = "Start date is required.")
  @FutureOrPresent(message = "Start date cannot be in the past.")
  private LocalDate startDate;
}
