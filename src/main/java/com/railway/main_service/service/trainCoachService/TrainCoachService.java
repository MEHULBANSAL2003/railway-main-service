package com.railway.main_service.service.trainCoachService;

import com.railway.main_service.dto.request.common.ChangeStatusRequest;
import com.railway.main_service.dto.request.trainCoach.AddTrainCoachRequest;
import com.railway.main_service.dto.request.trainCoach.UpdateTrainCoachRequest;
import com.railway.main_service.dto.response.trainCoach.CoachTypeDropdownResponse;
import com.railway.main_service.dto.response.trainCoach.TrainCoachResponse;
import com.railway.main_service.dto.response.trainCoach.TrainCopyCoachesResponse;

import java.util.List;

public interface TrainCoachService {

  TrainCoachResponse addCoach(String trainNumber, AddTrainCoachRequest request);

  TrainCoachResponse updateCoach(String trainNumber, Long coachId, UpdateTrainCoachRequest request);

  TrainCoachResponse changeStatus(String trainNumber, Long coachId, ChangeStatusRequest request);

  // Active coaches today (effectiveFrom <= today <= effectiveTo or null)
  List<TrainCoachResponse> getAllByTrain(String trainNumber);

  // Deactivated coaches (effectiveTo in the past)
  List<TrainCoachResponse> getInactiveByTrain(String trainNumber);

  List<CoachTypeDropdownResponse> getAvailableCoachTypes(String trainNumber);

  TrainCopyCoachesResponse copyCoaches(String sourceTrainNumber, String targetTrainNumber);

  List<TrainCoachResponse> getCoachHistory(String trainNumber, String coachTypeCode);

  List<TrainCoachResponse> getAllByTrainIncludingInactive(String trainNumber);
}
