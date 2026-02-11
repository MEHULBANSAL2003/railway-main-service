package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.common.logging.Loggable;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.Pagination.PageRequestDto;
import com.railway.main_service.dto.request.station.AddNewStationRequest;
import com.railway.main_service.dto.response.pagination.PageResponseDto;
import com.railway.main_service.dto.response.station.AddNewStationResponse;
import com.railway.main_service.dto.response.station.StationResponse;
import com.railway.main_service.service.stationService.StationServiceImpl;
import com.railway.main_service.utility.excel.ExcelUploadResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping(ApiConstants.STATIONS)
@Loggable
@RequiredArgsConstructor
public class StationController {

  private final StationServiceImpl stationService;

  @PostMapping(ApiConstants.ADD_NEW_STATION)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<AddNewStationResponse>> addNewStation(@Valid @RequestBody AddNewStationRequest request) {
    AddNewStationResponse response = stationService.addNewStation(request);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @GetMapping(ApiConstants.GET_STATIONS)
  public ResponseEntity<ApiResponse<PageResponseDto<StationResponse>>> getAllStations(
    @Valid @ModelAttribute PageRequestDto pageRequest) {

    PageResponseDto<StationResponse> response = stationService.getAllStations(pageRequest);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PostMapping(ApiConstants.UPLOAD_STATIONS_EXCEL)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<ExcelUploadResult>> uploadStationsExcel(
    @RequestParam("file") MultipartFile file) {

    ExcelUploadResult result = stationService.uploadStationsExcel(file);
    return ResponseEntity.ok(ApiResponse.success(result));
  }
}
