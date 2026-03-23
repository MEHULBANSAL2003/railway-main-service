package com.railway.main_service.service.coachTypeService;

import com.railway.main_service.dto.request.ActivateRequest;
import com.railway.main_service.dto.request.DeactivateRequest;
import com.railway.main_service.dto.request.coachType.AddCoachTypeRequest;
import com.railway.main_service.dto.request.coachType.UpdateCoachTypeRequest;
import com.railway.main_service.dto.response.DeactivationResponse;
import com.railway.main_service.dto.response.PeriodResponse;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.coachType.CoachTypeResponse;

import java.util.List;

public interface CoachTypeService {

  CoachTypeResponse addCoachType(AddCoachTypeRequest request);

  CoachTypeResponse updateCoachType(String typeCode, UpdateCoachTypeRequest request);

  DeactivationResponse deactivate(String typeCode, DeactivateRequest request);

  DeactivationResponse activate(String typeCode, ActivateRequest request);

  List<PeriodResponse> getPeriods(String typeCode);

  List<CoachTypeResponse> getAllForDropdown(String search);

  List<CoachTypeResponse> getAllForAdmin(String search);

  CascadeInfoResponse getCascadeInfo(String typeCode);
}
