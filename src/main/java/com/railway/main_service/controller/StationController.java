package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.common.logging.Loggable;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.station.AddNewStationRequest;
import com.railway.main_service.dto.response.station.AddNewStationResponse;
import com.railway.main_service.service.stationService.StationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(ApiConstants.STATIONS)
@Loggable
@RequiredArgsConstructor
public class StationController {

  private final StationServiceImpl stationService;

  @PostMapping(ApiConstants.ADD_NEW_STATION)
  public ResponseEntity<ApiResponse<AddNewStationResponse>> addNewStation(AddNewStationRequest request) {
    AddNewStationResponse response = stationService.addNewStation(request);

    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
