package com.railway.main_service.mapper;

import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.response.station.StationResponse;
import com.railway.main_service.entity.StationEntity;

public class StationMapper {

  private StationMapper() {
    throw new IllegalStateException("Mapper class");
  }

  public static StationResponse toDto(StationEntity entity) {
    boolean isSuperAdmin = SecurityUtils.isSuperAdmin();

    return StationResponse.builder()
      .stationId(entity.getId())
      .stationCode(entity.getStationCode())
      .stationName(entity.getStationName())
      // City
      .cityId(entity.getCity().getId())
      .cityName(entity.getCity().getName())
      // State (through city relationship)
      .stateId(entity.getCity().getState().getId())
      .stateName(entity.getCity().getState().getName())
      .stateCode(entity.getCity().getState().getCode())
      // Zone
      .zoneId(entity.getZone().getId())
      .zoneName(entity.getZone().getName())
      .zoneCode(entity.getZone().getCode())
      // Station details
      .stationType(entity.getStationType())
      .numPlatforms(entity.getNumPlatforms())
      .latitude(entity.getLatitude())
      .longitude(entity.getLongitude())
      .isActive(entity.getIsActive())
      // Permissions
      .canUpdatedByCurrentAdmin(isSuperAdmin)
      .canDeletedByCurrentAdmin(isSuperAdmin)
      // Timestamps
      .createdAt(entity.getCreatedAt())
      .updatedAt(entity.getUpdatedAt())
      .build();
  }
}
