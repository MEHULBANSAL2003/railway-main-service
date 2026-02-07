package com.railway.main_service.dto.request.station;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddNewStationRequest {

  @NotBlank(message = "Station Code is required")
  private String station_code;

  @NotBlank(message = "Station Name is required")
  private String station_name;

  @NotBlank(message = "City is required")
  private String city;

  @NotBlank(message = "State is required")
  private String state;

  @NotBlank(message = "Zone is required")
  private String zone;

  @NotBlank(message = "Country is required")
  private int num_platforms;

  @NotBlank(message = "Is Junction is required")
  private boolean is_junction;

}
