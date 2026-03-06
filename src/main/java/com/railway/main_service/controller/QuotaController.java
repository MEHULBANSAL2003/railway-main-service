package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.quota.AddQuotaRequest;
import com.railway.main_service.dto.request.quota.UpdateQuotaRequest;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.quota.QuotaResponse;
import com.railway.main_service.service.quotaService.QuotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

  @PostMapping(ApiConstants.QUOTA_STATUS)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<QuotaResponse>> toggleStatus(
    @PathVariable String quotaCode,
    @RequestParam boolean isActive) {
    return ResponseEntity.ok(ApiResponse.success(
      quotaService.toggleStatus(quotaCode, isActive)));
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
