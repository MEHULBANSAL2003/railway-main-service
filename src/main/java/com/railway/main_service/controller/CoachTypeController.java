package com.railway.main_service.controller;


import com.railway.common.exceptions.ApiResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.ActivateRequest;
import com.railway.main_service.dto.request.DeactivateRequest;
import com.railway.main_service.dto.request.coachType.AddCoachTypeRequest;
import com.railway.main_service.dto.request.coachType.UpdateCoachTypeRequest;
import com.railway.main_service.dto.response.DeactivationResponse;
import com.railway.main_service.dto.response.PeriodResponse;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.coachType.CoachTypeResponse;
import com.railway.main_service.service.coachTypeService.CoachTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.COACH_TYPES)
@RequiredArgsConstructor
public class CoachTypeController {

  private final CoachTypeService coachTypeService;

  @PostMapping(ApiConstants.ADD_COACH_TYPE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CoachTypeResponse>> addCoachType(
    @Valid @RequestBody AddCoachTypeRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      coachTypeService.addCoachType(request)));
  }

  @PostMapping(ApiConstants.UPDATE_COACH_TYPE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CoachTypeResponse>> updateCoachType(
    @PathVariable String typeCode,
    @Valid @RequestBody UpdateCoachTypeRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      coachTypeService.updateCoachType(typeCode, request)));
  }

  @PostMapping(ApiConstants.DEACTIVATE_COACH_TYPE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<DeactivationResponse>> deactivate(
    @PathVariable String typeCode,
    @RequestBody @Valid DeactivateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      coachTypeService.deactivate(typeCode, request)));
  }

  @PostMapping(ApiConstants.ACTIVATE_COACH_TYPE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<DeactivationResponse>> activate(
    @PathVariable String typeCode,
    @RequestBody @Valid ActivateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      coachTypeService.activate(typeCode, request)));
  }

  @GetMapping(ApiConstants.COACH_TYPE_PERIODS)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<PeriodResponse>>> getPeriods(
    @PathVariable String typeCode) {
    return ResponseEntity.ok(ApiResponse.success(
      coachTypeService.getPeriods(typeCode)));
  }

  @GetMapping(ApiConstants.GET_COACH_TYPES)
  public ResponseEntity<ApiResponse<List<CoachTypeResponse>>> getAllForDropdown(
    @RequestParam(required = false) String search
  ) {
    return ResponseEntity.ok(ApiResponse.success(
      coachTypeService.getAllForDropdown(search)));
  }

  @GetMapping(ApiConstants.GET_COACH_TYPES_ADMIN)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<CoachTypeResponse>>> getAllForAdmin(
    @RequestParam(required = false) String search) {
    return ResponseEntity.ok(ApiResponse.success(
      coachTypeService.getAllForAdmin(search)));
  }

  @GetMapping(ApiConstants.CASCADE_OPS)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CascadeInfoResponse>> getCascadeInfo(
    @PathVariable String typeCode) {
    return ResponseEntity.ok(ApiResponse.success(
      coachTypeService.getCascadeInfo(typeCode)));
  }
}
