package com.railway.main_service.dto.response.city;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddCityResponse {
  private Long cityId;
  private String cityName;
  private Long stateId;
  private String stateName;
  private String stateCode;
  private Boolean isActive;
  private LocalDateTime createdAt;
  private String message;
}
