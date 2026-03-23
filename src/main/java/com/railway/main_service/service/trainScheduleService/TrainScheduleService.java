package com.railway.main_service.service.trainScheduleService;

import com.railway.main_service.dto.request.DeactivateRequest;
import com.railway.main_service.dto.request.trainSchedule.AddTrainScheduleRequest;
import com.railway.main_service.dto.response.trainSchedule.TrainScheduleResponse;
import com.railway.main_service.dto.response.trainSchedule.TrainScheduleSummaryResponse;

public interface TrainScheduleService {

  // Single summary call — running + upcoming + past
  TrainScheduleSummaryResponse getSummary(String trainNumber);

  // Create new schedule
  TrainScheduleResponse createSchedule(String trainNumber, AddTrainScheduleRequest request);

  // Deactivate a schedule by setting endDate
  TrainScheduleResponse deactivateSchedule(String trainNumber, Long scheduleId, DeactivateRequest request);
}
