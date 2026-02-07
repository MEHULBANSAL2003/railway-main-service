package com.railway.main_service.service.stationService;

import com.railway.common.logging.Loggable;
import com.railway.main_service.dto.request.station.AddNewStationRequest;
import com.railway.main_service.dto.response.station.AddNewStationResponse;
import org.springframework.stereotype.Service;


@Service
@Loggable
public class StationServiceImpl implements StationService{
  @Override
  public AddNewStationResponse addNewStation(AddNewStationRequest request) {
    return null;
  }
}
