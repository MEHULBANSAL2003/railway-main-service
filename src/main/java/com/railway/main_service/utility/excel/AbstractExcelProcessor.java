package com.railway.main_service.utility.excel;

import com.railway.common.exceptions.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractExcelProcessor<T, E> {

  /**
   * Main processing method with smart batch processing
   */
  public ExcelUploadResult<E> processExcelFile(MultipartFile file) {

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
      validateHeaders(sheet);

      int totalRows = sheet.getPhysicalNumberOfRows() - 1;
      result.setTotalRows(totalRows);

      // Map to track which row each DTO came from
      Map<T, Integer> dtoToRowMap = new HashMap<>();
      List<T> validDtos = new ArrayList<>();
      int failureCount = 0;

      // PHASE 1: Parse and validate all rows (in-memory validation)
      log.info("Phase 1: Parsing and validating {} rows...", totalRows);
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);

        if (row == null || isRowEmpty(row)) {
          continue;
        }

        try {
          // Parse row
          T dto = parseRow(row, rowIndex);

          // Basic validation (no DB calls)
          List<String> validationErrors = validateDto(dto);

          if (!validationErrors.isEmpty()) {
            for (String error : validationErrors) {
              result.getErrors().add(ExcelUploadResult.UploadError.builder()
                .rowNumber(rowIndex + 1)
                .field("validation")
                .errorMessage(error)
                .build());
            }
            failureCount++;
            continue;
          }

          validDtos.add(dto);
          dtoToRowMap.put(dto, rowIndex + 1);

        } catch (Exception e) {
          log.error("Error parsing row {}: {}", rowIndex + 1, e.getMessage());
          result.getErrors().add(ExcelUploadResult.UploadError.builder()
            .rowNumber(rowIndex + 1)
            .field("parsing")
            .errorMessage("Parse error: " + e.getMessage())
            .build());
          failureCount++;
        }
      }

      log.info("Phase 1 complete: {} valid, {} invalid", validDtos.size(), failureCount);

      // PHASE 2: Database validation (check duplicates in batch)
      log.info("Phase 2: Checking for duplicates in database...");
      List<T> finalValidDtos = new ArrayList<>();

      for (T dto : validDtos) {
        List<String> dbErrors = validateDtoWithDatabase(dto);

        if (!dbErrors.isEmpty()) {
          Integer rowNum = dtoToRowMap.get(dto);
          for (String error : dbErrors) {
            result.getErrors().add(ExcelUploadResult.UploadError.builder()
              .rowNumber(rowNum)
              .field("database")
              .errorMessage(error)
              .build());
          }
          failureCount++;
        } else {
          finalValidDtos.add(dto);
        }
      }

      log.info("Phase 2 complete: {} valid, {} duplicates skipped",
        finalValidDtos.size(), validDtos.size() - finalValidDtos.size());

      // PHASE 3: Batch save (single transaction, fast!)
      if (!finalValidDtos.isEmpty()) {
        log.info("Phase 3: Batch saving {} records...", finalValidDtos.size());
        try {
          List<E> savedEntities = saveBatch(finalValidDtos);
          result.getSuccessRecords().addAll(savedEntities);
          result.setSuccessCount(savedEntities.size());
          log.info("Phase 3 complete: {} records saved successfully", savedEntities.size());
        } catch (Exception e) {
          log.error("Batch save failed: {}", e.getMessage(), e);
          throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR,
            "BATCH_SAVE_ERROR",
            "Failed to save records: " + e.getMessage());
        }
      }

      result.setFailureCount(failureCount);
      result.setMessage(String.format(
        "Upload completed: %d success, %d failed out of %d total rows",
        result.getSuccessCount(), result.getFailureCount(), result.getTotalRows()
      ));

    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error processing Excel file: {}", e.getMessage(), e);
      throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR,
        "UPLOAD_ERROR", "Error processing Excel file: " + e.getMessage());
    }

    return result;
  }

  private boolean isRowEmpty(Row row) {
    for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
      if (row.getCell(cellNum) != null &&
        !ExcelHelper.getCellValue(row.getCell(cellNum)).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  // Abstract methods
  protected abstract void validateHeaders(Sheet sheet);
  protected abstract T parseRow(Row row, int rowIndex);
  protected abstract List<String> validateDto(T dto);
  protected abstract List<E> saveBatch(List<T> dtoList);
  protected abstract List<String> getRequiredHeaders();

  /**
   * Database validation - override this to check duplicates
   * Default: no validation (for backward compatibility)
   */
  protected List<String> validateDtoWithDatabase(T dto) {
    return new ArrayList<>();
  }
}
