package com.railway.main_service.service.stationService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.station.AddNewStationRequest;
import com.railway.main_service.dto.response.station.AddNewStationResponse;
import com.railway.main_service.entity.StationEntity;
import com.railway.main_service.repository.StationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


@Service
@Loggable
@Slf4j
@RequiredArgsConstructor
public class StationServiceImpl implements StationService{

  private final StationRepository stationRepository;

  @Override
  @Transactional
  public AddNewStationResponse addNewStation(AddNewStationRequest request) {

     boolean isStationCodeExists = stationRepository.existsByStationCode(request.getStationCode());

     if (isStationCodeExists){
       throw new BaseException(HttpStatus.CONFLICT,"STATION_ALREADY_EXISTS","Station already exists");
     }

    StationEntity station = StationEntity.builder()
      .stationCode(request.getStationCode())  // Fixed field names
      .stationName(request.getStationName())
      .city(request.getCity())
      .state(request.getState())
      .zone(request.getZone())
      .numPlatforms(request.getNumPlatforms())
      .isJunction(request.isJunction())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    StationEntity savedStation = stationRepository.save(station);

    return AddNewStationResponse.builder()
      .stationId(savedStation.getId())
      .stationCode(savedStation.getStationCode())
      .stationName(savedStation.getStationName())
      .city(savedStation.getCity())
      .state(savedStation.getState())
      .zone(savedStation.getZone())
      .numPlatforms(savedStation.getNumPlatforms())
      .isJunction(savedStation.isJunction())
      .createdBy(savedStation.getCreatedBy())
      .createdAt(savedStation.getCreatedAt())
      .message("Station created successfully")
      .build();
  }
}
