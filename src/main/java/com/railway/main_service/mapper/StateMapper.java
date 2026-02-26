package com.railway.main_service.mapper;

import com.railway.main_service.dto.response.state.StateResponse;
import com.railway.main_service.entity.StateEntity;

public class StateMapper {

  private StateMapper() {
    throw new IllegalStateException("Mapper class");
  }

  public static StateResponse toDto(StateEntity entity) {
    if (entity == null) {
      return null;
    }

    return StateResponse.builder()
      .id(entity.getId())
      .code(entity.getCode())
      .name(entity.getName())
      .isActive(entity.getIsActive())
      .createdAt(entity.getCreatedAt())
      .build();
  }
}
