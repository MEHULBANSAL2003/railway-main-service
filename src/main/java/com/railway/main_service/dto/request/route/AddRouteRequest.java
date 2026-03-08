package com.railway.main_service.dto.request.route;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddRouteRequest {

  @NotBlank(message = "Route code is required.")
  @Size(max = 20, message = "Route code max 20 characters.")
  private String routeCode;

  @NotBlank(message = "Route name is required.")
  @Size(min = 3, max = 150, message = "Route name must be 3–150 characters.")
  private String routeName;
}
