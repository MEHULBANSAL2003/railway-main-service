package com.railway.main_service.service.coachTypeService;

import com.railway.main_service.dto.request.coachType.AddCoachTypeRequest;
import com.railway.main_service.dto.request.coachType.UpdateCoachTypeRequest;
import com.railway.main_service.dto.response.coachType.CoachTypeResponse;

import java.util.List;

public interface CoachTypeService {

  public CoachTypeResponse addCoachType(AddCoachTypeRequest request);

  public CoachTypeResponse updateCoachType(String typeCode, UpdateCoachTypeRequest request);


  public CoachTypeResponse toggleStatus(String typeCode, boolean isActive);

  public List<CoachTypeResponse> getAllForDropdown();

  public List<CoachTypeResponse> getAllForAdmin(String search);

}
