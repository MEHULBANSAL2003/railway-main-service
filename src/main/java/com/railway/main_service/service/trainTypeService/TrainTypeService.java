package com.railway.main_service.service.trainTypeService;

import com.railway.main_service.dto.request.common.ChangeStatusRequest;
import com.railway.main_service.dto.request.trainType.AddTrainTypeRequest;
import com.railway.main_service.dto.request.trainType.SetAllowedCoachesRequest;
import com.railway.main_service.dto.request.trainType.UpdateTrainTypeRequest;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.trainType.AllowedCoachResponse;
import com.railway.main_service.dto.response.trainType.TrainTypeResponse;

import java.util.List;

public interface TrainTypeService {

  // ── Existing ──────────────────────────────────────────────
  TrainTypeResponse  addTrainType(AddTrainTypeRequest request);
  TrainTypeResponse  updateTrainType(String typeCode, UpdateTrainTypeRequest request);
  TrainTypeResponse  changeStatus(String typeCode, ChangeStatusRequest request);
  CascadeInfoResponse getCascadeInfo(String typeCode);
  List<TrainTypeResponse> getAllForDropdown(String search);
  List<TrainTypeResponse> getAllForAdmin(String search);

  // ── New ───────────────────────────────────────────────────
  List<AllowedCoachResponse> getAllowedCoaches(String typeCode);
  List<AllowedCoachResponse> setAllowedCoaches(String typeCode, SetAllowedCoachesRequest request);
}
