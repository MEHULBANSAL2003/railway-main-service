package com.railway.main_service.dto.request.common;

import com.railway.main_service.enums.ActiveStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeStatusRequest {

  @NotNull(message = "Status is required (ACTIVE or INACTIVE)")
  private ActiveStatus status;

  @NotNull(message = "Effective from date is required")
  @FutureOrPresent(message = "Effective from date must be today or in the future")
  private LocalDate effectiveFrom;

  @NotBlank(message = "Reason is required")
  @Size(max = 500, message = "Reason must not exceed 500 characters")
  private String reason;
}
