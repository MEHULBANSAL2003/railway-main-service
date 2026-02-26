package com.railway.main_service.service.cityService;

import com.railway.common.exceptions.BaseException;
import com.railway.main_service.entity.CityEntity;
import com.railway.main_service.entity.StateEntity;
import com.railway.main_service.repository.CityRepository;
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
public class CityExcelProcessor {

  private final CityRepository cityRepository;
  private final StateRepository stateRepository;

  private static final int MAX_FILE_SIZE_MB = 5;
  private static final int MAX_ROWS = 1000;

  @Transactional
  public ExcelUploadResult<CityEntity> processExcelFile(MultipartFile file) {

    // Step 1: Validate file
    validateFile(file);

    List<CityEntity> successRecords = new ArrayList<>();
    List<ExcelUploadResult.UploadError> errors = new ArrayList<>();
    List<CityEntity> citiesToSave = new ArrayList<>();
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
          // Parse city name and state name from row
          String cityName = getCellValueAsString(row.getCell(0));
          String stateName = getCellValueAsString(row.getCell(1));

          // Validate inputs
          validateCityName(cityName);
          validateStateName(stateName);

          String trimmedCityName = cityName.trim();
          String trimmedStateName = stateName.trim();

          // Step 1: Find state by name
          StateEntity state = stateRepository.findByNameIgnoreCase(trimmedStateName)
            .orElseThrow(() -> new IllegalArgumentException(
              "State not found: " + trimmedStateName
            ));

          // Step 2: Check if state is active
          if (!state.getIsActive()) {
            throw new IllegalArgumentException(
              "State '" + trimmedStateName + "' is inactive. Cannot add cities to an inactive state."
            );
          }

          // Step 3: Check if city already exists in this state (database)
          if (cityRepository.existsByNameIgnoreCaseAndStateId(trimmedCityName, state.getId())) {
            errors.add(ExcelUploadResult.UploadError.builder()
              .rowNumber(i + 1)
              .field("city_name")
              .value(trimmedCityName)
              .errorMessage("City '" + trimmedCityName + "' already exists in state '" + trimmedStateName + "'")
              .build());
            failureCount++;
            continue;
          }

          // Step 4: Check for duplicates within current batch (same city + state combo)
          boolean duplicateInBatch = citiesToSave.stream()
            .anyMatch(c ->
              c.getName().equalsIgnoreCase(trimmedCityName) &&
                c.getState().getId().equals(state.getId())
            );

          if (duplicateInBatch) {
            errors.add(ExcelUploadResult.UploadError.builder()
              .rowNumber(i + 1)
              .field("city_name")
              .value(trimmedCityName)
              .errorMessage("Duplicate city '" + trimmedCityName + "' in state '" + trimmedStateName + "' found in Excel file")
              .build());
            failureCount++;
            continue;
          }

          // Step 5: Parse is_active (optional, default true)
          Boolean isActive = true;
          Cell activeCell = row.getCell(2);
          if (activeCell != null) {
            String activeValue = getCellValueAsString(activeCell);
            if (activeValue != null && !activeValue.trim().isEmpty()) {
              isActive = parseBoolean(activeValue);
            }
          }

          // Step 6: Build CityEntity
          CityEntity city = CityEntity.builder()
            .name(trimmedCityName)
            .state(state)
            .isActive(isActive)
            .build();

          citiesToSave.add(city);

        } catch (IllegalArgumentException e) {
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

      // Step 7: Batch save all valid cities
      if (!citiesToSave.isEmpty()) {
        List<CityEntity> savedCities = cityRepository.saveAll(citiesToSave);
        successRecords.addAll(savedCities);
        successCount = savedCities.size();
        log.info("Successfully saved {} cities to database", successCount);
      }

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

    return ExcelUploadResult.<CityEntity>builder()
      .totalRows(totalRows - skippedRows)
      .successCount(successCount)
      .failureCount(failureCount)
      .successRecords(successRecords)
      .errors(errors)
      .message(buildResultMessage(successCount, failureCount, skippedRows))
      .build();
  }

  // ─── Validation ───────────────────────────────────────────────────────────

  private void validateCityName(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("City name is required");
    }
    String trimmed = name.trim();
    if (trimmed.length() < 2 || trimmed.length() > 100) {
      throw new IllegalArgumentException("City name must be between 2-100 characters");
    }
    if (!trimmed.matches("^[A-Za-z\\s]+$")) {
      throw new IllegalArgumentException("City name must contain only letters and spaces");
    }
  }

  private void validateStateName(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("State name is required");
    }
  }

  // ─── Cell Helpers ─────────────────────────────────────────────────────────

  private String getCellValueAsString(Cell cell) {
    if (cell == null) return null;

    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue().trim();
      case NUMERIC -> {
        if (DateUtil.isCellDateFormatted(cell)) {
          yield cell.getDateCellValue().toString();
        }
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
    if (value == null || value.trim().isEmpty()) return true;
    value = value.trim().toLowerCase();
    return value.equals("yes") || value.equals("true") ||
      value.equals("1") || value.equals("y") || value.equals("active");
  }

  private boolean isRowEmpty(Row row) {
    if (row == null) return true;
    for (int i = 0; i < 2; i++) {
      Cell cell = row.getCell(i);
      if (cell != null && cell.getCellType() != CellType.BLANK) {
        String value = getCellValueAsString(cell);
        if (value != null && !value.isEmpty()) return false;
      }
    }
    return true;
  }

  // ─── File Validation ──────────────────────────────────────────────────────

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "File cannot be empty");
    }

    String filename = file.getOriginalFilename();
    if (filename == null || filename.isEmpty()) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_FILENAME", "File must have a valid filename");
    }

    if (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls")) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_FILE_FORMAT", "Only Excel files (.xlsx, .xls) are allowed");
    }

    long fileSizeInMB = file.getSize() / (1024 * 1024);
    if (fileSizeInMB > MAX_FILE_SIZE_MB) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE",
        "File size exceeds maximum limit of " + MAX_FILE_SIZE_MB + "MB");
    }

    String contentType = file.getContentType();
    if (contentType != null &&
      !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") &&
      !contentType.equals("application/vnd.ms-excel")) {
      log.warn("Unexpected content type: {}. Proceeding with file extension validation.", contentType);
    }

    log.info("File validation passed. Filename: {}, Size: {} bytes", filename, file.getSize());
  }

  // ─── Error Helpers ────────────────────────────────────────────────────────

  private String extractFieldFromError(String errorMessage) {
    if (errorMessage == null) return "unknown";
    String lower = errorMessage.toLowerCase();
    if (lower.contains("city")) return "city_name";
    if (lower.contains("state")) return "state_name";
    if (lower.contains("active")) return "is_active";
    return "unknown";
  }

  private String extractValueFromRow(Row row, String field) {
    if (row == null) return "";
    try {
      return switch (field) {
        case "city_name" -> getCellValueAsString(row.getCell(0));
        case "state_name" -> getCellValueAsString(row.getCell(1));
        case "is_active" -> getCellValueAsString(row.getCell(2));
        default -> "";
      };
    } catch (Exception e) {
      return "";
    }
  }

  private String buildResultMessage(int success, int failed, int skipped) {
    StringBuilder message = new StringBuilder();
    message.append(success).append(" city/cities uploaded successfully");
    if (failed > 0) message.append(", ").append(failed).append(" failed");
    if (skipped > 0) message.append(", ").append(skipped).append(" row(s) skipped (empty)");
    return message.toString();
  }
}
