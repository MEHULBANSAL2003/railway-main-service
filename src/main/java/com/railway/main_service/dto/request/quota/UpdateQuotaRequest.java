package com.railway.main_service.dto.request.quota;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateQuotaRequest {

  @Size(min = 2, max = 50)
  private String quotaName;

  @Size(max = 255)
  private String description;
}
