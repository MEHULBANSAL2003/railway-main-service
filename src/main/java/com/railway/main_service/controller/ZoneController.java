package com.railway.main_service.controller;

import com.railway.common.excel.ExcelUploadResult;
import com.railway.common.excel.ExcelUploadService;
import com.railway.common.exceptions.ApiResponse;
import com.railway.common.logging.Loggable;
import com.railway.common.pagination.FilterRequest;
import com.railway.common.pagination.PagedResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.zone.CreateZoneRequest;
import com.railway.main_service.dto.request.zone.UpdateZoneRequest;
import com.railway.main_service.dto.request.zone.ZoneStatusRequest;
import com.railway.main_service.dto.response.zone.ZoneResponse;
import com.railway.main_service.excel.ZoneExcelRowMapper;
import com.railway.main_service.service.zoneService.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping(ApiConstants.ZONE_BASE_V1)
@RequiredArgsConstructor
public class ZoneController {

  private final ZoneService zoneService;
  private final ExcelUploadService excelUploadService;
  private final ZoneExcelRowMapper zoneExcelRowMapper;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<ZoneResponse>> createZone(
    @Valid @RequestBody CreateZoneRequest request) {
    log.info("Create zone request received");
    return ResponseEntity.ok(ApiResponse.success(zoneService.createZone(request)));
  }

  @PostMapping(ApiConstants.GET_UPDATE_ZONE_DETAIL)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<ZoneResponse>> updateZone(
    @PathVariable Long zoneId,
    @Valid @RequestBody UpdateZoneRequest request) {
    log.info("Update zone request received for id: {}", zoneId);
    return ResponseEntity.ok(ApiResponse.success(zoneService.updateZone(zoneId, request)));
  }

  @PostMapping(ApiConstants.DEACTIVATE_ZONE)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<ZoneResponse>> deactivateZone(
    @PathVariable Long zoneId,
    @Valid @RequestBody ZoneStatusRequest request) {
    log.info("Deactivate zone request received for id: {}", zoneId);
    return ResponseEntity.ok(ApiResponse.success(zoneService.deactivateZone(zoneId, request)));
  }

  @PostMapping(ApiConstants.REACTIVATE_ZONE)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<ZoneResponse>> reactivateZone(
    @PathVariable Long zoneId,
    @Valid @RequestBody ZoneStatusRequest request) {
    log.info("Reactivate zone request received for id: {}", zoneId);
    return ResponseEntity.ok(ApiResponse.success(zoneService.reactivateZone(zoneId, request)));
  }

  @GetMapping(ApiConstants.GET_UPDATE_ZONE_DETAIL)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<ZoneResponse>> getZoneById(
    @PathVariable Long zoneId) {
    log.info("Get zone request received for id: {}", zoneId);
    return ResponseEntity.ok(ApiResponse.success(zoneService.getActiveZoneById(zoneId)));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<PagedResponse<ZoneResponse>>> getAllZones(
    @ModelAttribute FilterRequest request) {
    log.info("Get all zones request received");
    return ResponseEntity.ok(ApiResponse.success(zoneService.getAllZones(request)));
  }

  @PostMapping(ApiConstants.UPLOAD_ZONE_EXCEL)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<ExcelUploadResult>> uploadZones(
    @RequestParam("file") MultipartFile file) {
    log.info("Excel upload request received for zones");
    return ResponseEntity.ok(ApiResponse.success(
      excelUploadService.process(file, zoneExcelRowMapper, zoneService::createZoneFromExcel)
    ));
  }


}
