package com.railway.main_service.service.stationService;

import com.railway.main_service.dto.request.station.AddNewStationRequest;
import com.railway.main_service.dto.response.station.AddNewStationResponse;

public interface StationService {

  AddNewStationResponse addNewStation(AddNewStationRequest request);
}
