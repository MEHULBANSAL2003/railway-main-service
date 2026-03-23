package com.railway.main_service.service.trainCoachService;

import com.railway.main_service.dto.request.DeactivateRequest;
import com.railway.main_service.dto.request.trainCoach.AddTrainCoachRequest;
import com.railway.main_service.dto.request.trainCoach.UpdateTrainCoachRequest;
import com.railway.main_service.dto.response.DeactivationResponse;
import com.railway.main_service.dto.response.trainCoach.CoachTypeDropdownResponse;
import com.railway.main_service.dto.response.trainCoach.TrainCoachResponse;
import com.railway.main_service.dto.response.trainCoach.TrainCopyCoachesResponse;

import java.util.List;

public interface TrainCoachService {

  // Add a coach type to a train
  TrainCoachResponse addCoach(String trainNumber, AddTrainCoachRequest request);

  // Update count / tatkalSeats / racSeats / waitlistLimit — coachType is immutable
  TrainCoachResponse updateCoach(String trainNumber, Long coachId, UpdateTrainCoachRequest request);

  // Deactivate a coach (set effectiveTo, validate no booking conflicts)
  DeactivationResponse deactivate(String trainNumber, Long coachId, DeactivateRequest request);

  // Active coaches for a train (sub-page load)
  List<TrainCoachResponse> getAllByTrain(String trainNumber);

  // Inactive (past effective period) coaches for a train
  List<TrainCoachResponse> getInactiveByTrain(String trainNumber);

  // Full history — all coaches regardless of effective dates
  List<TrainCoachResponse> getCoachHistory(String trainNumber);

  // Available coach types that can still be added to this train
  List<CoachTypeDropdownResponse> getAvailableCoachTypes(String trainNumber);

  // Copy coach configuration from one train to another
  TrainCopyCoachesResponse copyCoaches(String sourceTrainNumber, String targetTrainNumber);

  // All coaches for a train including inactive ones
  List<TrainCoachResponse> getAllByTrainIncludingInactive(String trainNumber);
}
