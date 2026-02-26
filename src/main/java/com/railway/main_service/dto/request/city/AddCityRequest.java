package com.railway.main_service.dto.request.city;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddCityRequest {

  @NotBlank(message = "City name is required")
  @Size(min = 2, max = 100, message = "City name must be between 2 and 100 characters")
  private String cityName;

  @NotBlank(message = "State name is required")
  @Size(min = 2, max = 100, message = "State name must be between 2 and 100 characters")
  private String stateName;
}
