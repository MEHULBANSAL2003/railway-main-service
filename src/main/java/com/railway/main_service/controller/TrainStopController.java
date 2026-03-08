package com.railway.main_service.controller;


import com.railway.common.exceptions.ApiResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.trainStop.AddTrainStopRequest;
import com.railway.main_service.dto.request.trainStop.UpdateTrainStopRequest;
import com.railway.main_service.dto.response.trainStop.TrainStopResponse;
import com.railway.main_service.service.trainStopService.TrainStopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.TRAIN_STOPS)
@RequiredArgsConstructor
public class TrainStopController {

  private final TrainStopService trainStopService;


  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainStopResponse>>> getAllStops(
    @PathVariable String trainNumber) {
    return ResponseEntity.ok(
      ApiResponse.success(trainStopService.getAllByTrain(trainNumber)));
  }

  @PostMapping(ApiConstants.ADD_NEW_TRAIN_STOP)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainStopResponse>> addStop(
    @PathVariable String trainNumber,
    @Valid @RequestBody AddTrainStopRequest request) {
    return ResponseEntity.ok(
      ApiResponse.success(trainStopService.addStop(trainNumber, request)));
  }


  @PostMapping(ApiConstants.UPDATE_TRAIN_STOP)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainStopResponse>> updateStop(
    @PathVariable String trainNumber,
    @PathVariable Long stopId,
    @Valid @RequestBody UpdateTrainStopRequest request) {
    return ResponseEntity.ok(
      ApiResponse.success(trainStopService.updateStop(trainNumber, stopId, request)));
  }

  @PostMapping(ApiConstants.DELETE_TRAIN_STOP)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> deleteStop(
    @PathVariable String trainNumber,
    @PathVariable Long stopId) {
    trainStopService.deleteStop(trainNumber, stopId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
