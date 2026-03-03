package com.railway.main_service.dto.request.station;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteStationRequest {
  @NotBlank(message = "Delete reason is required.")
  @Size(min = 5, max = 255, message = "Reason must be between 5 and 255 characters.")
  private String reason;
}
