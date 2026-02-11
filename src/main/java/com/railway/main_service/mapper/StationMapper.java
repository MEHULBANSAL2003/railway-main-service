package com.railway.main_service.mapper;

import com.railway.main_service.dto.response.station.StationResponse;
import com.railway.main_service.entity.StationEntity;

public class StationMapper {

  /**
   * Converts StationEntity to StationResponseDto
   */
  public static StationResponse toDto(StationEntity entity) {
    return StationResponse.builder()
      .stationId(entity.getId())
      .stationCode(entity.getStationCode())
      .stationName(entity.getStationName())
      .city(entity.getCity())
      .state(entity.getState())
      .zone(entity.getZone())
      .numPlatforms(entity.getNumPlatforms())
      .isJunction(entity.isJunction())
      .createdAt(entity.getCreatedAt())
      .canDeletedByCurrentAdmin(false)
      .canUpdatedByCurrentAdmin(false)
      .build();
  }

}
