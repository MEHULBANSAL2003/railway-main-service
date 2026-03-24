package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.trainType.AddTrainTypeRequest;
import com.railway.main_service.dto.request.trainType.SetAllowedCoachesRequest;
import com.railway.main_service.dto.request.trainType.UpdateTrainTypeRequest;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.trainType.AllowedCoachResponse;
import com.railway.main_service.dto.response.trainType.TrainTypeResponse;
import com.railway.main_service.dto.request.common.ChangeStatusRequest;
import com.railway.main_service.service.trainTypeService.TrainTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.TRAIN_TYPES)
@RequiredArgsConstructor
public class TrainTypeController {

  private final TrainTypeService trainTypeService;

  @PostMapping(ApiConstants.ADD_TRAIN_TYPE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainTypeResponse>> addTrainType(
    @Valid @RequestBody AddTrainTypeRequest request) {

    return ResponseEntity.ok(ApiResponse.success(trainTypeService.addTrainType(request)));
  }

  @PostMapping(ApiConstants.UPDATE_TRAIN_TYPE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainTypeResponse>> updateTrainType(
    @PathVariable String typeCode,
    @Valid @RequestBody UpdateTrainTypeRequest request) {
    return ResponseEntity.ok(ApiResponse.success(trainTypeService.updateTrainType(typeCode, request)));
  }

  @PostMapping(ApiConstants.CHANGE_STATUS)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainTypeResponse>> changeStatus(
    @PathVariable String typeCode,
    @Valid @RequestBody ChangeStatusRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainTypeService.changeStatus(typeCode, request)));
  }

  @GetMapping(ApiConstants.GET_TRAIN_TYPES)
  public ResponseEntity<ApiResponse<List<TrainTypeResponse>>> getAllForDropdown(
    @RequestParam(required = false) String search
  ) {
    return ResponseEntity.ok(ApiResponse.success(
      trainTypeService.getAllForDropdown(search)));
  }

  @GetMapping(ApiConstants.GET_TRAIN_TYPES_ADMIN)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainTypeResponse>>> getAllForAdmin(
    @RequestParam(required = false) String search) {
    return ResponseEntity.ok(ApiResponse.success(
      trainTypeService.getAllForAdmin(search)));
  }
  @GetMapping(ApiConstants.CASCADE_OPS)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CascadeInfoResponse>> getCascadeInfo(
    @PathVariable String typeCode) {
    return ResponseEntity.ok(ApiResponse.success(
      trainTypeService.getCascadeInfo(typeCode)));
  }

  @GetMapping(ApiConstants.TRAIN_TYPE_ALLOWED_COACHES)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<AllowedCoachResponse>>> getAllowedCoaches(
    @PathVariable String typeCode) {
    return ResponseEntity.ok(ApiResponse.success(
      trainTypeService.getAllowedCoaches(typeCode)));
  }

  // PUT  /api/main/train-types/{typeCode}/allowed-coaches
// Full replace — send complete desired list
  @PostMapping(ApiConstants.TRAIN_TYPE_ALLOWED_COACHES)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<AllowedCoachResponse>>> setAllowedCoaches(
    @PathVariable String typeCode,
    @Valid @RequestBody SetAllowedCoachesRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainTypeService.setAllowedCoaches(typeCode, request)));
  }
}
