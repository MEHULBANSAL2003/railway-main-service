package com.railway.main_service.service.trainCoachService;

import com.railway.main_service.dto.request.trainCoach.AddTrainCoachRequest;
import com.railway.main_service.dto.request.trainCoach.UpdateTrainCoachRequest;
import com.railway.main_service.dto.response.trainCoach.CoachTypeDropdownResponse;
import com.railway.main_service.dto.response.trainCoach.TrainCoachResponse;
import java.util.List;

public interface TrainCoachService {

  // Add a coach type to a train
  TrainCoachResponse addCoach(String trainNumber, AddTrainCoachRequest request);

  // Update count / tatkalSeats — coachType is immutable
  TrainCoachResponse updateCoach(String trainNumber, Long coachId, UpdateTrainCoachRequest request);

  // Soft toggle
  TrainCoachResponse toggleStatus(String trainNumber, Long coachId, boolean isActive);

  // All coaches for a train (sub-page load)
  List<TrainCoachResponse> getAllByTrain(String trainNumber);

  List<CoachTypeDropdownResponse> getAvailableCoachTypes(String trainNumber);
}
