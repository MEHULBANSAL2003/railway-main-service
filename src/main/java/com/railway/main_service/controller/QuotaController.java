package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.ActivateRequest;
import com.railway.main_service.dto.request.DeactivateRequest;
import com.railway.main_service.dto.request.quota.AddQuotaRequest;
import com.railway.main_service.dto.request.quota.UpdateQuotaRequest;
import com.railway.main_service.dto.response.DeactivationResponse;
import com.railway.main_service.dto.response.PeriodResponse;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.quota.QuotaResponse;
import com.railway.main_service.service.quotaService.QuotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.QUOTAS)
@RequiredArgsConstructor
public class QuotaController {

  private final QuotaService quotaService;

  @PostMapping(ApiConstants.ADD_QUOTA)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<QuotaResponse>> addQuota(
    @Valid @RequestBody AddQuotaRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
      .body(ApiResponse.success(quotaService.addQuota(request)));
  }

  @PostMapping(ApiConstants.UPDATE_QUOTA)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<QuotaResponse>> updateQuota(
    @PathVariable String quotaCode,
    @Valid @RequestBody UpdateQuotaRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      quotaService.updateQuota(quotaCode, request)));
  }

  @PostMapping(ApiConstants.DEACTIVATE_QUOTA)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<DeactivationResponse>> deactivate(
    @PathVariable String quotaCode,
    @RequestBody @Valid DeactivateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      quotaService.deactivate(quotaCode, request)));
  }

  @PostMapping(ApiConstants.ACTIVATE_QUOTA)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<DeactivationResponse>> activate(
    @PathVariable String quotaCode,
    @RequestBody @Valid ActivateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      quotaService.activate(quotaCode, request)));
  }

  @GetMapping(ApiConstants.QUOTA_PERIODS)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<PeriodResponse>>> getPeriods(
    @PathVariable String quotaCode) {
    return ResponseEntity.ok(ApiResponse.success(
      quotaService.getPeriods(quotaCode)));
  }

  @GetMapping(ApiConstants.GET_QUOTAS_DROPDOWN)
  public ResponseEntity<ApiResponse<List<QuotaResponse>>> getAllForDropdown() {
    return ResponseEntity.ok(ApiResponse.success(quotaService.getAllForDropdown()));
  }

  @GetMapping(ApiConstants.GET_QUOTAS_ADMIN)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<QuotaResponse>>> getAllForAdmin() {
    return ResponseEntity.ok(ApiResponse.success(quotaService.getAllForAdmin()));
  }

  @GetMapping(ApiConstants.CASCADE_OPS)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CascadeInfoResponse>> getCascadeInfo(
    @PathVariable String typeCode) {
    return ResponseEntity.ok(ApiResponse.success(
      quotaService.getCascadeInfo(typeCode)));
  }
}
