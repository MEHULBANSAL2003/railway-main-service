package com.railway.main_service.dto.request.route;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateRouteRequest {

  // routeCode is immutable after creation
  @Size(min = 3, max = 150, message = "Route name must be 3–150 characters.")
  private String routeName;
}
