package com.railway.main_service.controller;


import com.railway.common.exceptions.ApiResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.trainSchedule.AddTrainScheduleRequest;
import com.railway.main_service.dto.response.trainSchedule.TrainScheduleResponse;
import com.railway.main_service.dto.response.trainSchedule.TrainScheduleSummaryResponse;
import com.railway.main_service.service.trainScheduleService.TrainScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstants.TRAIN_SCHEDULE)
@RequiredArgsConstructor
public class TrainScheduleController {

  private final TrainScheduleService scheduleService;

  // GET /api/main/trains/{trainNumber}/schedules/summary
  // Single call — returns running, upcoming[], past[], deactivated[]
  @GetMapping("/summary")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainScheduleSummaryResponse>> getSummary(
    @PathVariable String trainNumber) {
    return ResponseEntity.ok(
      ApiResponse.success(scheduleService.getSummary(trainNumber)));
  }

  // POST /api/main/trains/{trainNumber}/schedules/add
  @PostMapping("/add")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainScheduleResponse>> createSchedule(
    @PathVariable String trainNumber,
    @Valid @RequestBody AddTrainScheduleRequest request) {
    return ResponseEntity.ok(
      ApiResponse.success(scheduleService.createSchedule(trainNumber, request)));
  }

  // PATCH /api/main/trains/{trainNumber}/schedules/{scheduleId}/toggle
  // Toggle isActive — cannot toggle RUNNING schedule
  @PostMapping("/{scheduleId}/toggle")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainScheduleResponse>> toggleSchedule(
    @PathVariable String trainNumber,
    @PathVariable Long scheduleId) {
    return ResponseEntity.ok(
      ApiResponse.success(scheduleService.toggleSchedule(trainNumber, scheduleId)));
  }
}
