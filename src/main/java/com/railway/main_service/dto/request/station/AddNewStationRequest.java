package com.railway.main_service.dto.request.station;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddNewStationRequest {

  @NotBlank(message = "Station Code is required")
  @Size(min = 2, max = 5, message = "Station Code must be between 2 and 5 characters")
  @Pattern(regexp = "^[A-Z]{2,5}$", message = "Station Code must be 2-5 uppercase letters only")
  private String stationCode;

  @NotBlank(message = "Station Name is required")
  @Size(min = 3, max = 100, message = "Station Name must be between 3 and 100 characters")
  @Pattern(regexp = "^[A-Za-z\\s\\-.()]+$", message = "Station Name can only contain letters, spaces, hyphens, dots, and parentheses")
  private String stationName;

  @NotBlank(message = "City is required")
  private String city;

  @NotBlank(message = "State is required")
  private String state;

  @NotBlank(message = "Zone is required")
  private String zone;

  @Min(value = 1, message = "Number of platforms must be at least 1")  // Remove @NotBlank
  @Max(value = 25, message = "Number of platforms cannot exceed 25")
  private int numPlatforms;

  private boolean isJunction;  // Remove @NotBlank - booleans can't be blank
}
