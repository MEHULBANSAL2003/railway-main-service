package com.railway.main_service.utility.excel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelUploadResult<T> {

  private int totalRows;
  private int successCount;
  private int failureCount;

  @Builder.Default
  private List<T> successRecords = new ArrayList<>();

  @Builder.Default
  private List<UploadError> errors = new ArrayList<>();

  private String message;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UploadError {
    private int rowNumber;
    private String field;
    private String value;
    private String errorMessage;
  }
}
