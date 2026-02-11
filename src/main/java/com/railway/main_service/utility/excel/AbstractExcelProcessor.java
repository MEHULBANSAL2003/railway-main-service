package com.railway.main_service.utility.excel;

import com.railway.common.exceptions.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractExcelProcessor<T, E> {

  /**
   * Main method to process Excel file
   */
  public ExcelUploadResult<E> processExcelFile(MultipartFile file) {

    // Validate file
    if (file.isEmpty()) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "Uploaded file is empty");
    }

    if (!ExcelHelper.hasExcelFormat(file)) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_FORMAT",
        "Invalid file format. Please upload .xlsx or .xls file");
    }

    ExcelUploadResult<E> result = ExcelUploadResult.<E>builder()
      .successRecords(new ArrayList<>())
      .errors(new ArrayList<>())
      .successCount(0)
      .failureCount(0)
      .build();

    try (Workbook workbook = ExcelHelper.createWorkbook(file)) {

      Sheet sheet = workbook.getSheetAt(0);

      // Validate headers
      validateHeaders(sheet);

      int totalRows = sheet.getPhysicalNumberOfRows() - 1; // Exclude header
      result.setTotalRows(totalRows);

      List<T> dtoList = new ArrayList<>();
      int successCount = 0;
      int failureCount = 0;

      // Process each row (skip header row)
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);

        if (row == null || isRowEmpty(row)) {
          continue;
        }

        try {
          // Parse row to DTO
          T dto = parseRow(row, rowIndex);

          // Validate DTO
          List<String> validationErrors = validateDto(dto);
          if (!validationErrors.isEmpty()) {
            for (String error : validationErrors) {
              result.getErrors().add(ExcelUploadResult.UploadError.builder()
                .rowNumber(rowIndex + 1)
                .errorMessage(error)
                .build());
            }
            failureCount++;
            continue;
          }

          dtoList.add(dto);

        } catch (Exception e) {
          log.error("Error processing row {}: {}", rowIndex + 1, e.getMessage());
          result.getErrors().add(ExcelUploadResult.UploadError.builder()
            .rowNumber(rowIndex + 1)
            .errorMessage(e.getMessage())
            .build());
          failureCount++;
        }
      }

      // Save valid records in batch
      if (!dtoList.isEmpty()) {
        List<E> savedEntities = saveBatch(dtoList);
        result.getSuccessRecords().addAll(savedEntities);
        successCount = savedEntities.size();
      }

      result.setSuccessCount(successCount);
      result.setFailureCount(failureCount);
      result.setMessage(String.format(
        "Upload completed: %d success, %d failed out of %d total rows",
        successCount, failureCount, totalRows
      ));

    } catch (Exception e) {
      log.error("Error processing Excel file: {}", e.getMessage(), e);
      throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR,
        "UPLOAD_ERROR", "Error processing Excel file: " + e.getMessage());
    }

    return result;
  }

  /**
   * Check if row is empty
   */
  private boolean isRowEmpty(Row row) {
    for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
      if (row.getCell(cellNum) != null &&
        !ExcelHelper.getCellValue(row.getCell(cellNum)).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Abstract methods to be implemented by specific processors
   */
  protected abstract void validateHeaders(Sheet sheet);

  protected abstract T parseRow(Row row, int rowIndex);

  protected abstract List<String> validateDto(T dto);

  protected abstract List<E> saveBatch(List<T> dtoList);

  protected abstract List<String> getRequiredHeaders();
}
