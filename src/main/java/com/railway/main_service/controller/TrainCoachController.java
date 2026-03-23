package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.DeactivateRequest;
import com.railway.main_service.dto.request.ActivateRequest;
import com.railway.main_service.dto.request.trainCoach.AddTrainCoachRequest;
import com.railway.main_service.dto.request.trainCoach.CopyCoachesRequest;
import com.railway.main_service.dto.request.trainCoach.UpdateTrainCoachRequest;
import com.railway.main_service.dto.response.DeactivationResponse;
import com.railway.main_service.dto.response.PeriodResponse;
import com.railway.main_service.dto.response.trainCoach.CoachTypeDropdownResponse;
import com.railway.main_service.dto.response.trainCoach.TrainCoachResponse;
import com.railway.main_service.dto.response.trainCoach.TrainCopyCoachesResponse;
import com.railway.main_service.service.trainCoachService.TrainCoachService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.TRAIN_COACHES)          // /api/main/trains
@RequiredArgsConstructor
public class TrainCoachController {

  private final TrainCoachService trainCoachService;

  // GET  /api/main/trains/{trainNumber}/coaches
  @GetMapping(ApiConstants.GET_TRAIN_COACHES)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainCoachResponse>>> getAllCoaches(
    @PathVariable String trainNumber) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachService.getAllByTrain(trainNumber)));
  }

  // POST /api/main/trains/{trainNumber}/coaches/add
  @PostMapping(ApiConstants.TRAIN_COACH_ADD)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainCoachResponse>> addCoach(
    @PathVariable String trainNumber,
    @Valid @RequestBody AddTrainCoachRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachService.addCoach(trainNumber, request)));
  }

  // PATCH /api/main/trains/{trainNumber}/coaches/{coachId}
  @PostMapping(ApiConstants.TRAIN_COACH_UPDATE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainCoachResponse>> updateCoach(
    @PathVariable String trainNumber,
    @PathVariable Long coachId,
    @Valid @RequestBody UpdateTrainCoachRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachService.updateCoach(trainNumber, coachId, request)));
  }

  // POST /api/main/trains/{trainNumber}/coaches/{coachId}/deactivate
  @PostMapping(ApiConstants.DEACTIVATE_TRAIN_COACH)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<DeactivationResponse>> deactivate(
    @PathVariable String trainNumber,
    @PathVariable Long coachId,
    @RequestBody @Valid DeactivateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachService.deactivate(trainNumber, coachId, request)));
  }

  @GetMapping(ApiConstants.TRAIN_COACH_AVAILABLE_TYPES)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<CoachTypeDropdownResponse>>> getAvailableCoachTypes(
    @PathVariable String trainNumber) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachService.getAvailableCoachTypes(trainNumber)));
  }

  @PostMapping(ApiConstants.TRAIN_COACH_COPY_COACHES)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainCopyCoachesResponse>> copyCoaches(
    @PathVariable String trainNumber,
    @Valid @RequestBody CopyCoachesRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachService.copyCoaches(trainNumber, request.getTargetTrainNumber())));
  }
}
