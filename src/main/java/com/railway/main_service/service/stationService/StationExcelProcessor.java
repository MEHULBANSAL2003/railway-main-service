package com.railway.main_service.service.stationService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.station.StationExcelDto;
import com.railway.main_service.entity.StationEntity;
import com.railway.main_service.repository.StationRepository;
import com.railway.main_service.utility.excel.AbstractExcelProcessor;
import com.railway.main_service.utility.excel.ExcelHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StationExcelProcessor extends AbstractExcelProcessor<StationExcelDto, StationEntity> {

  private final StationRepository stationRepository;

  private static final List<String> REQUIRED_HEADERS = Arrays.asList(
    "station_code",
    "station_name",
    "city",
    "state",
    "latitude",
    "longitude",
    "zone",
    "is_junction",
    "num_platforms",
    "is_active"
  );

  @Override
  protected List<String> getRequiredHeaders() {
    return REQUIRED_HEADERS;
  }

  @Override
  protected void validateHeaders(Sheet sheet) {
    List<String> actualHeaders = ExcelHelper.getHeaders(sheet);

    if (actualHeaders.isEmpty()) {
      throw new BaseException(HttpStatus.BAD_REQUEST,
        "INVALID_HEADERS", "Excel file has no headers");
    }

    List<String> missingHeaders = new ArrayList<>();
    for (String required : REQUIRED_HEADERS) {
      if (!actualHeaders.contains(required)) {
        missingHeaders.add(required);
      }
    }

    if (!missingHeaders.isEmpty()) {
      throw new BaseException(HttpStatus.BAD_REQUEST,
        "INVALID_HEADERS",
        "Missing required headers: " + String.join(", ", missingHeaders));
    }
  }

  @Override
  protected StationExcelDto parseRow(Row row, int rowIndex) {
    return StationExcelDto.builder()
      .stationCode(ExcelHelper.getCellValue(row.getCell(0)))
      .stationName(ExcelHelper.getCellValue(row.getCell(1)))
      .city(ExcelHelper.getCellValue(row.getCell(2)))
      .state(ExcelHelper.getCellValue(row.getCell(3)))
      .latitude(ExcelHelper.getCellValueAsDouble(row.getCell(4)))
      .longitude(ExcelHelper.getCellValueAsDouble(row.getCell(5)))
      .zone(ExcelHelper.getCellValue(row.getCell(6)))
      .isJunction(ExcelHelper.getCellValueAsBoolean(row.getCell(7)))
      .numPlatforms(ExcelHelper.getCellValueAsInteger(row.getCell(8)))
      .isActive(ExcelHelper.getCellValueAsBoolean(row.getCell(9)))
      .build();
  }

  @Override
  protected List<String> validateDto(StationExcelDto dto) {
    List<String> errors = new ArrayList<>();

    // Basic validation
    if (dto.getStationCode() == null || dto.getStationCode().isEmpty()) {
      errors.add("Station Code is required");
    } else if (!dto.getStationCode().matches("^[A-Z]{2,5}$")) {
      errors.add("Station Code must be 2-5 uppercase letters");
    }

    if (dto.getStationName() == null || dto.getStationName().length() < 3) {
      errors.add("Station Name must be at least 3 characters");
    }

    if (dto.getCity() == null || dto.getCity().isEmpty()) {
      errors.add("City is required");
    }

    if (dto.getState() == null || dto.getState().isEmpty()) {
      errors.add("State is required");
    }

    if (dto.getLatitude() != null && (dto.getLatitude() < -90 || dto.getLatitude() > 90)) {
      errors.add("Latitude must be between -90 and 90");
    }

    if (dto.getLongitude() != null && (dto.getLongitude() < -180 || dto.getLongitude() > 180)) {
      errors.add("Longitude must be between -180 and 180");
    }

    if (dto.getZone() == null || dto.getZone().isEmpty()) {
      errors.add("Zone is required");
    }

    if (dto.getNumPlatforms() == null) {
      errors.add("Number of Platforms is required");
    } else if (dto.getNumPlatforms() < 1 || dto.getNumPlatforms() > 25) {
      errors.add("Number of Platforms must be between 1 and 25");
    }

    if (dto.getIsJunction() == null) {
      errors.add("Is Junction is required");
    }

    return errors;
  }

  /**
   * CRITICAL: This method checks for duplicates BEFORE attempting batch save
   * This allows us to skip duplicates and still batch-save valid records
   */
  @Override
  protected List<String> validateDtoWithDatabase(StationExcelDto dto) {
    List<String> errors = new ArrayList<>();

    // Check database for duplicates
    if (dto.getStationCode() != null &&
      stationRepository.existsByStationCode(dto.getStationCode())) {
      errors.add("Station Code '" + dto.getStationCode() + "' already exists in database");
    }

    if (dto.getStationName() != null &&
      stationRepository.existsByStationName(dto.getStationName())) {
      errors.add("Station Name '" + dto.getStationName() + "' already exists in database");
    }

    return errors;
  }

  /**
   * Batch save - Fast and efficient!
   * Only called with pre-validated records (no duplicates)
   */
  @Override
  @Transactional
  protected List<StationEntity> saveBatch(List<StationExcelDto> dtoList) {
    if (dtoList.isEmpty()) {
      return new ArrayList<>();
    }

    Long currentAdminId = SecurityUtils.getCurrentAdminId();

    // Convert all DTOs to Entities
    List<StationEntity> entities = dtoList.stream()
      .map(dto -> StationEntity.builder()
        .stationCode(dto.getStationCode())
        .stationName(dto.getStationName())
        .city(dto.getCity())
        .state(dto.getState())
        .latitude(dto.getLatitude())
        .longitude(dto.getLongitude())
        .zone(dto.getZone())
        .numPlatforms(dto.getNumPlatforms())
        .isJunction(dto.getIsJunction())
        .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
        .createdBy(currentAdminId)
        .build())
      .collect(Collectors.toList());

    // Single batch insert - FAST!
    return stationRepository.saveAll(entities);
  }
}
