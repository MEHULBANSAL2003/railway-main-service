package com.railway.main_service.service.coachTypeService;

import com.railway.main_service.dto.request.common.ChangeStatusRequest;
import com.railway.main_service.dto.request.coachType.AddCoachTypeRequest;
import com.railway.main_service.dto.request.coachType.UpdateCoachTypeRequest;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.coachType.CoachTypeResponse;

import java.util.List;

public interface CoachTypeService {

  public CoachTypeResponse addCoachType(AddCoachTypeRequest request);

  public CoachTypeResponse updateCoachType(String typeCode, UpdateCoachTypeRequest request);


  public CoachTypeResponse changeStatus(String typeCode, ChangeStatusRequest request);

  public List<CoachTypeResponse> getAllForDropdown(String search);

  public List<CoachTypeResponse> getAllForAdmin(String search);

  public CascadeInfoResponse getCascadeInfo(String typeCode);

}
