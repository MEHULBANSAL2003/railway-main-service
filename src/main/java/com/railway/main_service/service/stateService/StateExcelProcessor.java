package com.railway.main_service.service.stateService;

import com.railway.common.exceptions.BaseException;
import com.railway.main_service.entity.StateEntity;
import com.railway.main_service.repository.StateRepository;
import com.railway.main_service.utility.excel.ExcelUploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class StateExcelProcessor {

  private final StateRepository stateRepository;

  private static final int MAX_FILE_SIZE_MB = 5;
  private static final int MAX_ROWS = 1000;

  @Transactional
  public ExcelUploadResult<StateEntity> processExcelFile(MultipartFile file) {

    // Step 1: Validate file
    validateFile(file);

    List<StateEntity> successRecords = new ArrayList<>();
    List<ExcelUploadResult.UploadError> errors = new ArrayList<>();
    List<StateEntity> statesToSave = new ArrayList<>();
    int successCount = 0;
    int failureCount = 0;
    int totalRows = 0;
    int skippedRows = 0;

    try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

      Sheet sheet = workbook.getSheetAt(0);
      totalRows = sheet.getPhysicalNumberOfRows() - 1; // Exclude header

      // Validate row count
      if (totalRows > MAX_ROWS) {
        throw new BaseException(
          HttpStatus.BAD_REQUEST,
          "TOO_MANY_ROWS",
          "Excel file contains " + totalRows + " rows. Maximum allowed is " + MAX_ROWS
        );
      }

      log.info("Processing {} rows from Excel", totalRows);

      // Process each row
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);

        // Skip empty rows
        if (row == null || isRowEmpty(row)) {
          skippedRows++;
          continue;
        }

        try {
          // Parse state from row
          StateEntity state = parseStateFromRow(row, i);

          // Check for duplicate code in database
          if (stateRepository.existsByCode(state.getCode())) {
            errors.add(ExcelUploadResult.UploadError.builder()
              .rowNumber(i + 1)
              .field("code")
              .value(state.getCode())
              .errorMessage("State code already exists in database")
              .build());
            failureCount++;
            continue;
          }

          // Check for duplicate name in database
          if (stateRepository.existsByName(state.getName())) {
            errors.add(ExcelUploadResult.UploadError.builder()
              .rowNumber(i + 1)
              .field("name")
              .value(state.getName())
              .errorMessage("State name already exists in database")
              .build());
            failureCount++;
            continue;
          }

          // Check for duplicate in current batch
          boolean duplicateCodeInBatch = statesToSave.stream()
            .anyMatch(s -> s.getCode().equals(state.getCode()));

          if (duplicateCodeInBatch) {
            errors.add(ExcelUploadResult.UploadError.builder()
              .rowNumber(i + 1)
              .field("code")
              .value(state.getCode())
              .errorMessage("Duplicate code found in Excel file")
              .build());
            failureCount++;
            continue;
          }

          boolean duplicateNameInBatch = statesToSave.stream()
            .anyMatch(s -> s.getName().equals(state.getName()));

          if (duplicateNameInBatch) {
            errors.add(ExcelUploadResult.UploadError.builder()
              .rowNumber(i + 1)
              .field("name")
              .value(state.getName())
              .errorMessage("Duplicate name found in Excel file")
              .build());
            failureCount++;
            continue;
          }

          // Add to save list
          statesToSave.add(state);

        } catch (IllegalArgumentException e) {
          // Extract field name from error message if possible
          String field = extractFieldFromError(e.getMessage());
          String value = extractValueFromRow(row, field);

          errors.add(ExcelUploadResult.UploadError.builder()
            .rowNumber(i + 1)
            .field(field)
            .value(value)
            .errorMessage(e.getMessage())
            .build());
          failureCount++;
          log.error("Validation error at row {}: {}", i + 1, e.getMessage());
        } catch (Exception e) {
          errors.add(ExcelUploadResult.UploadError.builder()
            .rowNumber(i + 1)
            .field("unknown")
            .value("")
            .errorMessage("Unexpected error: " + e.getMessage())
            .build());
          failureCount++;
          log.error("Unexpected error at row {}: {}", i + 1, e.getMessage());
        }
      }

      // Step 2: Batch save all valid states
      if (!statesToSave.isEmpty()) {
        List<StateEntity> savedStates = stateRepository.saveAll(statesToSave);
        successRecords.addAll(savedStates);
        successCount = savedStates.size();
        log.info("Successfully saved {} states to database", successCount);
      }

      // Step 3: Log summary
      log.info("Excel processing completed. Success: {}, Failed: {}, Skipped: {}",
        successCount, failureCount, skippedRows);

    } catch (IOException e) {
      log.error("Error reading Excel file: {}", e.getMessage());
      throw new BaseException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "FILE_READ_ERROR",
        "Failed to read Excel file: " + e.getMessage()
      );
    }

    return ExcelUploadResult.<StateEntity>builder()
      .totalRows(totalRows - skippedRows)
      .successCount(successCount)
      .failureCount(failureCount)
      .successRecords(successRecords)
      .errors(errors)
      .message(buildResultMessage(successCount, failureCount, skippedRows))
      .build();
  }

  private StateEntity parseStateFromRow(Row row, int rowIndex) {

    // Column 0: code (REQUIRED)
    String code = getCellValueAsString(row.getCell(0));
    validateCode(code);

    // Column 1: name (REQUIRED)
    String name = getCellValueAsString(row.getCell(1));
    validateName(name);

    // Column 2: is_active (OPTIONAL, default true)
    Boolean isActive = true;
    Cell activeCell = row.getCell(2);
    if (activeCell != null) {
      String activeValue = getCellValueAsString(activeCell);
      if (activeValue != null && !activeValue.trim().isEmpty()) {
        isActive = parseBoolean(activeValue);
      }
    }

    return StateEntity.builder()
      .code(code.trim().toUpperCase())
      .name(name.trim())
      .isActive(isActive)
      .build();
  }

  private void validateCode(String code) {
    if (code == null || code.trim().isEmpty()) {
      throw new IllegalArgumentException("State code is required");
    }

    String trimmedCode = code.trim().toUpperCase();

    if (trimmedCode.length() < 2 || trimmedCode.length() > 5) {
      throw new IllegalArgumentException("State code must be between 2-5 characters");
    }

    if (!trimmedCode.matches("^[A-Z]{2,5}$")) {
      throw new IllegalArgumentException("State code must contain only uppercase letters");
    }
  }

  private void validateName(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("State name is required");
    }

    String trimmedName = name.trim();

    if (trimmedName.length() < 2 || trimmedName.length() > 100) {
      throw new IllegalArgumentException("State name must be between 2-100 characters");
    }

    if (!trimmedName.matches("^[A-Za-z\\s]+$")) {
      throw new IllegalArgumentException("State name must contain only letters and spaces");
    }
  }

  private String getCellValueAsString(Cell cell) {
    if (cell == null) {
      return null;
    }

    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue().trim();
      case NUMERIC -> {
        if (DateUtil.isCellDateFormatted(cell)) {
          yield cell.getDateCellValue().toString();
        }
        // Remove decimal for whole numbers
        double numValue = cell.getNumericCellValue();
        if (numValue == (long) numValue) {
          yield String.valueOf((long) numValue);
        }
        yield String.valueOf(numValue);
      }
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> {
        try {
          yield cell.getStringCellValue().trim();
        } catch (Exception e) {
          yield String.valueOf(cell.getNumericCellValue());
        }
      }
      case BLANK -> null;
      default -> null;
    };
  }

  private Boolean parseBoolean(String value) {
    if (value == null || value.trim().isEmpty()) {
      return true;
    }

    value = value.trim().toLowerCase();
    return value.equals("yes") ||
      value.equals("true") ||
      value.equals("1") ||
      value.equals("y") ||
      value.equals("active");
  }

  private boolean isRowEmpty(Row row) {
    if (row == null) {
      return true;
    }

    // Check first 2 required columns (code and name)
    for (int i = 0; i < 2; i++) {
      Cell cell = row.getCell(i);
      if (cell != null && cell.getCellType() != CellType.BLANK) {
        String value = getCellValueAsString(cell);
        if (value != null && !value.isEmpty()) {
          return false;
        }
      }
    }
    return true;
  }

  private void validateFile(MultipartFile file) {

    // Check if file is null or empty
    if (file == null || file.isEmpty()) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST,
        "EMPTY_FILE",
        "File cannot be empty"
      );
    }

    // Check filename
    String filename = file.getOriginalFilename();
    if (filename == null || filename.isEmpty()) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST,
        "INVALID_FILENAME",
        "File must have a valid filename"
      );
    }

    // Check file extension
    if (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls")) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST,
        "INVALID_FILE_FORMAT",
        "Only Excel files (.xlsx, .xls) are allowed"
      );
    }

    // Check file size (5MB max)
    long fileSizeInMB = file.getSize() / (1024 * 1024);
    if (fileSizeInMB > MAX_FILE_SIZE_MB) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST,
        "FILE_TOO_LARGE",
        "File size exceeds maximum limit of " + MAX_FILE_SIZE_MB + "MB"
      );
    }

    // Check content type
    String contentType = file.getContentType();
    if (contentType != null &&
      !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") &&
      !contentType.equals("application/vnd.ms-excel")) {
      log.warn("Unexpected content type: {}. Proceeding with file extension validation.", contentType);
    }

    log.info("File validation passed. Filename: {}, Size: {} bytes", filename, file.getSize());
  }

  private String extractFieldFromError(String errorMessage) {
    if (errorMessage == null) {
      return "unknown";
    }

    String lowerMessage = errorMessage.toLowerCase();
    if (lowerMessage.contains("code")) {
      return "code";
    } else if (lowerMessage.contains("name")) {
      return "name";
    } else if (lowerMessage.contains("active")) {
      return "is_active";
    }
    return "unknown";
  }

  private String extractValueFromRow(Row row, String field) {
    if (row == null) {
      return "";
    }

    try {
      return switch (field) {
        case "code" -> getCellValueAsString(row.getCell(0));
        case "name" -> getCellValueAsString(row.getCell(1));
        case "is_active" -> getCellValueAsString(row.getCell(2));
        default -> "";
      };
    } catch (Exception e) {
      return "";
    }
  }

  private String buildResultMessage(int success, int failed, int skipped) {
    StringBuilder message = new StringBuilder();

    message.append(success).append(" state(s) uploaded successfully");

    if (failed > 0) {
      message.append(", ").append(failed).append(" failed");
    }

    if (skipped > 0) {
      message.append(", ").append(skipped).append(" row(s) skipped (empty)");
    }

    return message.toString();
  }
}
