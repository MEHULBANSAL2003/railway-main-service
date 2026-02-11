package com.railway.main_service.service.stationService;

import com.railway.main_service.dto.request.Pagination.PageRequestDto;
import com.railway.main_service.dto.request.station.AddNewStationRequest;
import com.railway.main_service.dto.response.pagination.PageResponseDto;
import com.railway.main_service.dto.response.station.AddNewStationResponse;
import com.railway.main_service.dto.response.station.StationResponse;

public interface StationService {

  AddNewStationResponse addNewStation(AddNewStationRequest request);

  PageResponseDto<StationResponse> getAllStations(PageRequestDto pageRequest);
}
