package com.railway.main_service.dto.request.quota;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AddQuotaRequest {

  @NotBlank(message = "Quota code is required.")
  @Size(min = 1, max = 20, message = "Quota code must be between 1 and 20 characters.")
  private String quotaCode;

  @NotBlank(message = "Quota name is required.")
  @Size(min = 2, max = 50, message = "Quota name must be between 2 and 50 characters.")
  private String quotaName;

  @Size(max = 255)
  private String description;
}
