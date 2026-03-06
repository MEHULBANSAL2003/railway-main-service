package com.railway.main_service.service.trainTypeService;

import com.railway.main_service.dto.request.trainType.AddTrainTypeRequest;
import com.railway.main_service.dto.request.trainType.UpdateTrainTypeRequest;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.trainType.TrainTypeResponse;

import java.util.List;

public interface TrainTypeService {


  TrainTypeResponse addTrainType(AddTrainTypeRequest request);
  TrainTypeResponse updateTrainType(String typeCode, UpdateTrainTypeRequest request);
  TrainTypeResponse toggleStatus(String typeCode, boolean isActive);
  List<TrainTypeResponse> getAllForDropdown(String search);
  List<TrainTypeResponse> getAllForAdmin(String search);
  public CascadeInfoResponse getCascadeInfo(String typeCode);


}
