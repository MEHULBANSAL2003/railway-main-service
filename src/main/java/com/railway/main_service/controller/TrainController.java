package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.train.AddTrainRequest;
import com.railway.main_service.dto.request.train.UpdateTrainRequest;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.train.ReturnTrainResponse;
import com.railway.main_service.dto.response.train.TrainResponse;
import com.railway.main_service.service.trainService.TrainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.TRAINS)
@RequiredArgsConstructor
public class TrainController {

  private final TrainService trainService;

  @PostMapping(ApiConstants.ADD_TRAIN)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainResponse>> addTrain(
    @Valid @RequestBody AddTrainRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainService.addTrain(request)));
  }

  @PostMapping(ApiConstants.UPDATE_TRAIN)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainResponse>> updateTrain(
    @PathVariable String trainNumber,
    @Valid @RequestBody UpdateTrainRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainService.updateTrain(trainNumber, request)));
  }

  @PostMapping(ApiConstants.TRAIN_STATUS)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainResponse>> toggleStatus(
    @PathVariable String trainNumber,
    @RequestParam boolean isActive) {
    return ResponseEntity.ok(ApiResponse.success(
      trainService.toggleStatus(trainNumber, isActive)));
  }

  @GetMapping(ApiConstants.TRAIN_CASCADE_INFO)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CascadeInfoResponse>> getCascadeInfo(
    @PathVariable String trainNumber) {
    return ResponseEntity.ok(ApiResponse.success(
      trainService.getCascadeInfo(trainNumber)));
  }

  @GetMapping(ApiConstants.GET_TRAINS_ADMIN)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainResponse>>> getAllForAdmin(
    @RequestParam(required = false) String search) {
    return ResponseEntity.ok(ApiResponse.success(
      trainService.getAllForAdmin(search)));
  }

  @GetMapping(ApiConstants.GET_TRAINS_DROPDOWN)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainResponse>>> getAllForDropdown(
    @RequestParam(required = false) String search) {
    return ResponseEntity.ok(ApiResponse.success(
      trainService.getAllForDropdown(search)));
  }

  @GetMapping(ApiConstants.TRAIN_RETURN_INFO)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<ReturnTrainResponse>> getReturnTrainInfo(
    @PathVariable String trainNumber) {
    return ResponseEntity.ok(ApiResponse.success(
      trainService.getReturnTrainInfo(trainNumber)));
  }
}
