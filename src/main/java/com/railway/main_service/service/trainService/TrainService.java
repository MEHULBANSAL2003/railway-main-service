package com.railway.main_service.service.trainService;

import com.railway.main_service.dto.request.train.AddTrainRequest;
import com.railway.main_service.dto.request.train.UpdateTrainRequest;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.train.ReturnTrainResponse;
import com.railway.main_service.dto.response.train.TrainResponse;

import java.util.List;

public interface TrainService {

  TrainResponse addTrain(AddTrainRequest request);

  TrainResponse updateTrain(String trainNumber, UpdateTrainRequest request);

  TrainResponse toggleStatus(String trainNumber, boolean isActive);

  // Used by cascade modal before toggling — returns linked record counts
  CascadeInfoResponse getCascadeInfo(String trainNumber);

  List<TrainResponse> getAllForAdmin(String search);

  List<TrainResponse> getAllForDropdown(String search);

  ReturnTrainResponse getReturnTrainInfo(String trainNumber);
}
