package com.railway.main_service.service.trainScheduleService;

import com.railway.main_service.dto.request.trainSchedule.AddTrainScheduleRequest;
import com.railway.main_service.dto.response.trainSchedule.TrainScheduleResponse;
import com.railway.main_service.dto.response.trainSchedule.TrainScheduleSummaryResponse;

public interface TrainScheduleService {

  // Single summary call — running + upcoming + past + deactivated
  TrainScheduleSummaryResponse getSummary(String trainNumber);

  // Create new schedule
  TrainScheduleResponse createSchedule(String trainNumber, AddTrainScheduleRequest request);

  // Toggle isActive on a schedule (cannot toggle RUNNING)
  TrainScheduleResponse toggleSchedule(String trainNumber, Long scheduleId);
}
