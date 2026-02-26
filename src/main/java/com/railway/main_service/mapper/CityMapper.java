package com.railway.main_service.mapper;

import com.railway.main_service.dto.response.city.AddCityResponse;
import com.railway.main_service.dto.response.city.CityResponse;
import com.railway.main_service.entity.CityEntity;

public class CityMapper {

  private CityMapper() {
    throw new IllegalStateException("Mapper class");
  }

  public static CityResponse toCityResponse(CityEntity entity) {
    if (entity == null) {
      return null;
    }

    return CityResponse.builder()
      .id(entity.getId())
      .name(entity.getName())
      .stateId(entity.getState().getId())
      .stateName(entity.getState().getName())
      .stateCode(entity.getState().getCode())
      .isActive(entity.getIsActive())
      .createdAt(entity.getCreatedAt())
      .updatedAt(entity.getUpdatedAt())
      .build();
  }

  public static AddCityResponse toAddCityResponse(CityEntity entity) {
    if (entity == null) {
      return null;
    }

    return AddCityResponse.builder()
      .cityId(entity.getId())
      .cityName(entity.getName())
      .stateId(entity.getState().getId())
      .stateName(entity.getState().getName())
      .stateCode(entity.getState().getCode())
      .isActive(entity.getIsActive())
      .createdAt(entity.getCreatedAt())
      .message("City added successfully")
      .build();
  }
}
