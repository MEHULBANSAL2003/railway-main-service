package com.railway.main_service.dto.request.trainStop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class BulkAddTrainStopRequest {

  @NotNull(message = "Stops list is required.")
  @Size(min = 2, message = "At least 2 stops are required (source and destination).")
  @Valid
  private List<AddTrainStopRequest> stops;
}
