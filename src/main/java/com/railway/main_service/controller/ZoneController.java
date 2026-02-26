package com.railway.main_service.controller;


import com.railway.common.exceptions.ApiResponse;
import com.railway.common.logging.Loggable;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.zone.AddZoneRequest;
import com.railway.main_service.dto.response.zone.ZoneResponse;
import com.railway.main_service.service.zoneService.ZoneService;
import com.railway.main_service.service.zoneService.ZoneServiceImpl;
import com.railway.main_service.utility.excel.ExcelUploadResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.ZONES)
@RequiredArgsConstructor
@Loggable
public class ZoneController {

  private final ZoneService zoneService;


  @PostMapping(ApiConstants.ADD_ZONE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<ZoneResponse>> addNewZone(@Valid @RequestBody AddZoneRequest request) {
    ZoneResponse result = zoneService.addNewZone(request);
    return ResponseEntity.ok(ApiResponse.success(result));
  }


  @GetMapping(ApiConstants.GET_ZONES)
  public ResponseEntity<ApiResponse<List<ZoneResponse>>> getAllZones(@RequestParam(value = "searchTerm", required = false) String searchTerm) {
    List<ZoneResponse> result = zoneService.getAllZones(searchTerm);
    return ResponseEntity.ok(ApiResponse.success(result));
  }


}
