package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.ActivateRequest;
import com.railway.main_service.dto.request.DeactivateRequest;
import com.railway.main_service.dto.request.train.AddTrainRequest;
import com.railway.main_service.dto.request.train.UpdateTrainRequest;
import com.railway.main_service.dto.response.DeactivationResponse;
import com.railway.main_service.dto.response.PageResponse;
import com.railway.main_service.dto.response.PeriodResponse;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.train.BulkUploadResponse;
import com.railway.main_service.dto.response.train.ReturnTrainResponse;
import com.railway.main_service.dto.response.train.TrainResponse;
import com.railway.main_service.service.trainService.TrainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.TRAINS)
@RequiredArgsConstructor
public class TrainController {

  private final TrainService trainService;

  // ── CRUD ──────────────────────────────────────────────────────────────────

  @PostMapping(ApiConstants.ADD_TRAIN)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainResponse>> addTrain(
    @Valid @RequestBody AddTrainRequest request) {
    return ResponseEntity.ok(ApiResponse.success(trainService.addTrain(request)));
  }

  @PostMapping(ApiConstants.UPDATE_TRAIN)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainResponse>> updateTrain(
    @PathVariable String trainNumber,
    @Valid @RequestBody UpdateTrainRequest request) {
    return ResponseEntity.ok(ApiResponse.success(trainService.updateTrain(trainNumber, request)));
  }

  @PostMapping(ApiConstants.DEACTIVATE_TRAIN)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<DeactivationResponse>> deactivate(
    @PathVariable String trainNumber,
    @RequestBody @Valid DeactivateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainService.deactivate(trainNumber, request)));
  }

  @PostMapping(ApiConstants.ACTIVATE_TRAIN)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<DeactivationResponse>> activate(
    @PathVariable String trainNumber,
    @RequestBody @Valid ActivateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      trainService.activate(trainNumber, request)));
  }

  @GetMapping(ApiConstants.TRAIN_PERIODS)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<PeriodResponse>>> getPeriods(
    @PathVariable String trainNumber) {
    return ResponseEntity.ok(ApiResponse.success(
      trainService.getPeriods(trainNumber)));
  }

  // ── Queries ───────────────────────────────────────────────────────────────

  @GetMapping(ApiConstants.GET_TRAINS_ADMIN)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<TrainResponse>>> getAllForAdmin(
    @RequestParam(required = false)                    String  search,
    @RequestParam(required = false)                    String  trainTypeCode,
    @RequestParam(required = false)                    String  zoneCode,
    @RequestParam(required = false)                    Boolean isActive,
    @RequestParam(defaultValue = "1")                  int     page,
    @RequestParam(defaultValue = "20")                 int     size,
    @RequestParam(defaultValue = "trainNumber")        String  sortBy,
    @RequestParam(defaultValue = "asc")                String  sortDir
  ) {
    return ResponseEntity.ok(ApiResponse.success(
      trainService.getAllForAdmin(search, trainTypeCode, zoneCode, isActive,
        page, size, sortBy, sortDir)));
  }

  @GetMapping(ApiConstants.GET_TRAINS_DROPDOWN)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainResponse>>> getAllForDropdown(
    @RequestParam(required = false) String search) {
    return ResponseEntity.ok(ApiResponse.success(trainService.getAllForDropdown(search)));
  }

  @GetMapping(ApiConstants.TRAIN_CASCADE_INFO)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CascadeInfoResponse>> getCascadeInfo(
    @PathVariable String trainNumber) {
    return ResponseEntity.ok(ApiResponse.success(trainService.getCascadeInfo(trainNumber)));
  }

  @GetMapping(ApiConstants.TRAIN_RETURN_INFO)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<ReturnTrainResponse>> getReturnTrainInfo(
    @PathVariable String trainNumber) {
    return ResponseEntity.ok(ApiResponse.success(trainService.getReturnTrainInfo(trainNumber)));
  }

  // ── Excel ─────────────────────────────────────────────────────────────────

  @PostMapping(value = ApiConstants.TRAIN_UPLOAD, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<BulkUploadResponse>> uploadFromExcel(
    @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(ApiResponse.success(trainService.uploadFromExcel(file)));
  }

  @GetMapping(ApiConstants.TRAIN_UPLOAD_TEMPLATE)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<byte[]> getExcelTemplate() {
    byte[] template = trainService.getExcelTemplate();
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"trains_upload_template.xlsx\"")
      .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
      .body(template);
  }

  @GetMapping(ApiConstants.GET_TRAIN_DETAILS_BY_NUMBER)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<TrainResponse>> getTrainDetails(
    @PathVariable String trainNumber
  ){
    return ResponseEntity.ok(ApiResponse.success(trainService.getTrainDetails(trainNumber)));
  }
}
