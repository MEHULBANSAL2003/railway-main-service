package com.railway.main_service.mapper;

import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.response.station.StationResponse;
import com.railway.main_service.entity.StationEntity;

public class StationMapper {

  /**
   * Converts StationEntity to StationResponseDto
   */
  public static StationResponse toDto(StationEntity entity) {
    Boolean isSuperAdmin = SecurityUtils.isSuperAdmin();

    return StationResponse.builder()
      .stationId(entity.getId())
      .stationCode(entity.getStationCode())
      .stationName(entity.getStationName())
      .city("Bathinda")
      .state("Punjab")
      .zone("Northern")
      .numPlatforms(entity.getNumPlatforms())
      .isJunction(false)
      .createdAt(entity.getCreatedAt())
      .canDeletedByCurrentAdmin(isSuperAdmin)
      .canUpdatedByCurrentAdmin(isSuperAdmin)
      .build();
  }

}
