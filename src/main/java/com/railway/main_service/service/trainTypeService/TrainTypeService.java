package com.railway.main_service.service.trainTypeService;

import com.railway.main_service.dto.request.ActivateRequest;
import com.railway.main_service.dto.request.DeactivateRequest;
import com.railway.main_service.dto.request.trainType.AddTrainTypeRequest;
import com.railway.main_service.dto.request.trainType.SetAllowedCoachesRequest;
import com.railway.main_service.dto.request.trainType.UpdateTrainTypeRequest;
import com.railway.main_service.dto.response.DeactivationResponse;
import com.railway.main_service.dto.response.PeriodResponse;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.trainType.AllowedCoachResponse;
import com.railway.main_service.dto.response.trainType.TrainTypeResponse;

import java.util.List;

public interface TrainTypeService {

  // ── CRUD ────────────────────────────────────────────────
  TrainTypeResponse       addTrainType(AddTrainTypeRequest request);
  TrainTypeResponse       updateTrainType(String typeCode, UpdateTrainTypeRequest request);

  // ── Period-based activation / deactivation ──────────────
  DeactivationResponse    deactivate(String typeCode, DeactivateRequest request);
  DeactivationResponse    activate(String typeCode, ActivateRequest request);
  List<PeriodResponse>    getPeriods(String typeCode);

  // ── Cascade info ────────────────────────────────────────
  CascadeInfoResponse     getCascadeInfo(String typeCode);

  // ── Listings ────────────────────────────────────────────
  List<TrainTypeResponse> getAllForDropdown(String search);
  List<TrainTypeResponse> getAllForAdmin(String search);

  // ── Allowed coaches ─────────────────────────────────────
  List<AllowedCoachResponse> getAllowedCoaches(String typeCode);
  List<AllowedCoachResponse> setAllowedCoaches(String typeCode, SetAllowedCoachesRequest request);
}
