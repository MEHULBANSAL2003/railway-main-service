package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.Pagination.PageRequestDto;
import com.railway.main_service.dto.request.station.AddNewStationRequest;
import com.railway.main_service.dto.request.station.DeleteStationRequest;
import com.railway.main_service.dto.request.station.StationFilterRequest;
import com.railway.main_service.dto.request.station.UpdateStationRequest;
import com.railway.main_service.dto.response.pagination.PageResponseDto;
import com.railway.main_service.dto.response.station.AddNewStationResponse;
import com.railway.main_service.dto.response.station.DeleteStationResponse;
import com.railway.main_service.dto.response.station.RestoreDeletedStationResponse;
import com.railway.main_service.dto.response.station.StationResponse;
import com.railway.main_service.service.stationService.StationServiceImpl;
import com.railway.main_service.utility.excel.ExcelUploadResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


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
  @Valid @ModelAttribute StationFilterRequest filter) {
  PageResponseDto<StationResponse> response = stationService.getAllStations(filter);
  return ResponseEntity.ok(ApiResponse.success(response));
}

  @PostMapping(ApiConstants.UPLOAD_STATIONS_EXCEL)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<ExcelUploadResult>> uploadStationsExcel(
    @RequestParam("file") MultipartFile file) {

    ExcelUploadResult result = stationService.uploadStationsExcel(file);
    return ResponseEntity.ok(ApiResponse.success(result));
  }



  @PostMapping(ApiConstants.SET_ACTIVE_INACTIVE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<StationResponse>> changeStatus(@PathVariable String stationCode, @RequestParam(value = "activeStatus", required = true) boolean activeStatus){

    StationResponse response = stationService.updateActiveInactiveStatus(stationCode,activeStatus);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PostMapping(ApiConstants.UPDATE_STATION_DETAILS)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<AddNewStationResponse>> updateStationDetail(@PathVariable String stationCode, @RequestBody UpdateStationRequest request){

    AddNewStationResponse response = stationService.updateStationDetails(stationCode,request);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PostMapping(ApiConstants.DELETE_STATION)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<DeleteStationResponse>> deleteStation(@PathVariable String stationCode,
                                                                          @Valid @RequestBody DeleteStationRequest request){
    DeleteStationResponse response = stationService.deleteStation(stationCode, request);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @GetMapping(ApiConstants.GET_ALL_PERMANENTLY_DELETED_STATIONS)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<PageResponseDto<StationResponse>>> getAllPermanentlyDeletedStations(
    @Valid @ModelAttribute StationFilterRequest filter
  ){
    PageResponseDto<StationResponse> response = stationService.getAllPermanentlyDeletedStations(filter);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PostMapping(ApiConstants.RESTORE_DELETED_STATION)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<RestoreDeletedStationResponse>> restoreDeletedStations(
    @PathVariable String stationCode
  ){
    RestoreDeletedStationResponse response = stationService.restoreDeletedStation(stationCode);
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
