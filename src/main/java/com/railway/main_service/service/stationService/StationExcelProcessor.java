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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
      .stationCode(ExcelHelper.getCellValue(row.getCell(0)))           // station_code
      .stationName(ExcelHelper.getCellValue(row.getCell(1)))           // station_name
      .city(ExcelHelper.getCellValue(row.getCell(2)))                  // city
      .state(ExcelHelper.getCellValue(row.getCell(3)))                 // state
      .latitude(ExcelHelper.getCellValueAsDouble(row.getCell(4)))      // latitude
      .longitude(ExcelHelper.getCellValueAsDouble(row.getCell(5)))     // longitude
      .zone(ExcelHelper.getCellValue(row.getCell(6)))                  // zone
      .isJunction(ExcelHelper.getCellValueAsBoolean(row.getCell(7)))   // is_junction
      .numPlatforms(ExcelHelper.getCellValueAsInteger(row.getCell(8))) // num_platforms
      .isActive(ExcelHelper.getCellValueAsBoolean(row.getCell(9)))     // is_active
      .build();
  }

  @Override
  protected List<String> validateDto(StationExcelDto dto) {
    List<String> errors = new ArrayList<>();

    // Validate Station Code
    if (dto.getStationCode() == null || dto.getStationCode().isEmpty()) {
      errors.add("Station Code is required");
    } else if (!dto.getStationCode().matches("^[A-Z]{2,5}$")) {
      errors.add("Station Code must be 2-5 uppercase letters");
    } else if (stationRepository.existsByStationCode(dto.getStationCode())) {
      errors.add("Station Code " + dto.getStationCode() + " already exists");
    }

    // Validate Station Name
    if (dto.getStationName() == null || dto.getStationName().length() < 3) {
      errors.add("Station Name must be at least 3 characters");
    }

    // Validate City
    if (dto.getCity() == null || dto.getCity().isEmpty()) {
      errors.add("City is required");
    }

    // Validate State
    if (dto.getState() == null || dto.getState().isEmpty()) {
      errors.add("State is required");
    }

    // Validate Latitude (optional but if provided, must be valid)
    if (dto.getLatitude() != null && (dto.getLatitude() < -90 || dto.getLatitude() > 90)) {
      errors.add("Latitude must be between -90 and 90");
    }

    // Validate Longitude (optional but if provided, must be valid)
    if (dto.getLongitude() != null && (dto.getLongitude() < -180 || dto.getLongitude() > 180)) {
      errors.add("Longitude must be between -180 and 180");
    }

    // Validate Zone
    if (dto.getZone() == null || dto.getZone().isEmpty()) {
      errors.add("Zone is required");
    }

    // Validate Number of Platforms
    if (dto.getNumPlatforms() == null) {
      errors.add("Number of Platforms is required");
    } else if (dto.getNumPlatforms() < 1 || dto.getNumPlatforms() > 25) {
      errors.add("Number of Platforms must be between 1 and 25");
    }

    // Validate Is Junction
    if (dto.getIsJunction() == null) {
      errors.add("Is Junction is required");
    }

    return errors;
  }

  @Override
  @Transactional
  protected List<StationEntity> saveBatch(List<StationExcelDto> dtoList) {
    List<StationEntity> entities = new ArrayList<>();
    Long currentAdminId = SecurityUtils.getCurrentAdminId();

    // Convert DTOs to Entities
    for (StationExcelDto dto : dtoList) {
      StationEntity entity = StationEntity.builder()
        .stationCode(dto.getStationCode())
        .stationName(dto.getStationName())
        .city(dto.getCity())
        .state(dto.getState())
        .latitude(dto.getLatitude())           // Add this
        .longitude(dto.getLongitude())         // Add this
        .zone(dto.getZone())
        .numPlatforms(dto.getNumPlatforms())
        .isJunction(dto.getIsJunction())
        .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)  // Default to true
        .createdBy(currentAdminId)
        .build();

      entities.add(entity);
    }

    // Batch save
    return stationRepository.saveAll(entities);
  }
}
