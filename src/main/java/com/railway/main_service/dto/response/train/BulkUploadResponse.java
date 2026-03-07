package com.railway.main_service.dto.response.train;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkUploadResponse {

  private int successCount;
  private int failureCount;
  private int duplicateCount;

  // One entry per failed/skipped row
  private List<RowError> errors;

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class RowError {
    private int    rowNumber;   // Excel row number, 1-based (row 1 = header, data from row 2)
    private String trainNumber; // value from cell — may be blank or invalid
    private String trainName;   // value from cell
    private String reason;      // why this row failed
  }
}
