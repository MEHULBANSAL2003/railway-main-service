package com.railway.main_service.dto.response.zone;

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
public class ZoneResponse {
  private Long id;
  private String code;
  private String name;
  private Boolean isActive;
  private LocalDateTime createdAt;
}
