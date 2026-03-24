package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.common.ChangeStatusRequest;
import com.railway.main_service.dto.request.trainCoach.AddTrainCoachRequest;
import com.railway.main_service.dto.request.trainCoach.ChangeCoachConfigRequest;
import com.railway.main_service.dto.request.trainCoach.CopyCoachesRequest;
import com.railway.main_service.dto.request.trainCoach.DeactivateCoachRequest;
import com.railway.main_service.dto.request.trainCoach.ReactivateCoachRequest;
import com.railway.main_service.dto.request.trainCoach.UpdateTrainCoachRequest;
import com.railway.main_service.dto.response.trainCoach.CoachConfigChangeResponse;
import com.railway.main_service.dto.response.trainCoach.CoachTypeDropdownResponse;
import com.railway.main_service.dto.response.trainCoach.TrainCoachResponse;
import com.railway.main_service.dto.response.trainCoach.TrainCopyCoachesResponse;
import com.railway.main_service.service.trainCoachService.TrainCoachConfigService;
import com.railway.main_service.service.trainCoachService.TrainCoachService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.TRAIN_COACHES)
@RequiredArgsConstructor
public class TrainCoachController {

  private final TrainCoachService       trainCoachService;
  private final TrainCoachConfigService trainCoachConfigService;

  @GetMapping(ApiConstants.GET_TRAIN_COACHES)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainCoachResponse>>> getAllCoaches(
    @PathVariable String trainNumber) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachService.getAllByTrain(trainNumber)));
  }

  @PostMapping(ApiConstants.TRAIN_COACH_ADD)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainCoachResponse>> addCoach(
    @PathVariable String trainNumber,
    @Valid @RequestBody AddTrainCoachRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachService.addCoach(trainNumber, request)));
  }

  @PostMapping(ApiConstants.TRAIN_COACH_UPDATE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainCoachResponse>> updateCoach(
    @PathVariable String trainNumber,
    @PathVariable Long coachId,
    @Valid @RequestBody UpdateTrainCoachRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachService.updateCoach(trainNumber, coachId, request)));
  }

  @PostMapping(ApiConstants.TRAIN_COACH_STATUS)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainCoachResponse>> changeStatus(
    @PathVariable String trainNumber,
    @PathVariable Long coachId,
    @Valid @RequestBody ChangeStatusRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachService.changeStatus(trainNumber, coachId, request)));
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

  // Edit the existing row directly. Blocked if bookings exceed new limits.
  @PostMapping("/{trainNumber}/{coachId}/change-config")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CoachConfigChangeResponse>> changeConfig(
    @PathVariable String trainNumber,
    @PathVariable Long   coachId,
    @Valid @RequestBody ChangeCoachConfigRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachConfigService.changeConfig(trainNumber, coachId, request)));
  }

  // Sets effectiveTill = effectiveFrom. Removes unbooked future inventory.
  @PostMapping("/{trainNumber}/{coachId}/deactivate")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CoachConfigChangeResponse>> deactivate(
    @PathVariable String trainNumber,
    @PathVariable Long   coachId,
    @Valid @RequestBody DeactivateCoachRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachConfigService.deactivate(trainNumber, coachId, request)));
  }

  // Sets effectiveFrom = given date, effectiveTill = null.
  @PostMapping("/{trainNumber}/{coachId}/reactivate")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CoachConfigChangeResponse>> reactivate(
    @PathVariable String trainNumber,
    @PathVariable Long   coachId,
    @Valid @RequestBody ReactivateCoachRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachConfigService.reactivate(trainNumber, coachId, request)));
  }

  // Returns ALL rows (past, present, future) for a coach type on a train
  @GetMapping("/{trainNumber}/{coachTypeCode}/history")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainCoachResponse>>> getCoachHistory(
    @PathVariable String trainNumber,
    @PathVariable String coachTypeCode) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachService.getCoachHistory(trainNumber, coachTypeCode)));
  }

  @GetMapping("/{trainNumber}/inactive")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainCoachResponse>>> getInactiveCoaches(
    @PathVariable String trainNumber) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachService.getInactiveByTrain(trainNumber)));
  }

  @GetMapping("/{trainNumber}/coaches/all")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainCoachResponse>>> getAllCoachesIncludingInactive(
    @PathVariable String trainNumber) {
    return ResponseEntity.ok(ApiResponse.success(
      trainCoachService.getAllByTrainIncludingInactive(trainNumber)));
  }
}
