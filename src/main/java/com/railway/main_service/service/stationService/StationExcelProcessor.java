package com.railway.main_service.service.stationService;

import com.railway.main_service.dto.request.station.StationExcelDto;
import com.railway.main_service.entity.CityEntity;
import com.railway.main_service.entity.StationEntity;
import com.railway.main_service.entity.ZoneEntity;
import com.railway.main_service.enums.StationType;
import com.railway.main_service.repository.CityRepository;
import com.railway.main_service.repository.StationRepository;
import com.railway.main_service.repository.ZoneRepository;
import com.railway.main_service.utility.excel.AbstractExcelProcessor;
import com.railway.main_service.utility.excel.ExcelHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class StationExcelProcessor extends AbstractExcelProcessor<StationExcelDto, StationEntity> {

  private final StationRepository stationRepository;
  private final CityRepository cityRepository;
  private final ZoneRepository zoneRepository;

  private static final List<String> REQUIRED_HEADERS = List.of(
    "station_code", "station_name", "city_name", "state_name",
    "zone_name", "station_type", "num_platforms"
  );

  // ─── Caches loaded once per upload ───────────────────────────────────────
  private Map<String, CityEntity> cityCache;
  private Map<String, ZoneEntity> zoneCache;
  private Set<String> existingCodes;
  // ─────────────────────────────────────────────────────────────────────────

  @Override
  protected List<String> getRequiredHeaders() {
    return REQUIRED_HEADERS;
  }

  @Override
  protected void validateHeaders(Sheet sheet) {
    Row headerRow = sheet.getRow(0);
    if (headerRow == null) {
      throw new IllegalArgumentException("Excel file has no header row");
    }

    List<String> actualHeaders = new ArrayList<>();
    for (int i = 0; i < headerRow.getLastCellNum(); i++) {
      actualHeaders.add(ExcelHelper.getCellValue(headerRow.getCell(i)).toLowerCase().trim());
    }

    List<String> missing = REQUIRED_HEADERS.stream()
      .filter(h -> !actualHeaders.contains(h))
      .toList();

    if (!missing.isEmpty()) {
      throw new IllegalArgumentException("Missing required headers: " + missing);
    }

    // Load caches once — avoids N+1 DB calls during processing
    cityCache = cityRepository.findAll().stream()
      .collect(Collectors.toMap(
        c -> (c.getName().toLowerCase() + "|" + c.getState().getName().toLowerCase()),
        Function.identity(),
        (a, b) -> a  // keep first on duplicate
      ));

    zoneCache = zoneRepository.findAll().stream()
      .collect(Collectors.toMap(
        z -> z.getName().toLowerCase(),
        Function.identity()
      ));

    existingCodes = stationRepository.findAllStationCodes();

    log.info("Caches loaded — cities: {}, zones: {}, existing codes: {}",
      cityCache.size(), zoneCache.size(), existingCodes.size());
  }

  @Override
  protected StationExcelDto parseRow(Row row, int rowIndex) {
    return StationExcelDto.builder()
      .stationCode(ExcelHelper.getCellValue(row.getCell(0)).toUpperCase().trim())
      .stationName(ExcelHelper.getCellValue(row.getCell(1)).trim())
      .cityName(ExcelHelper.getCellValue(row.getCell(2)).trim())
      .stateName(ExcelHelper.getCellValue(row.getCell(3)).trim())
      .zoneName(ExcelHelper.getCellValue(row.getCell(4)).trim())
      .stationType(ExcelHelper.getCellValue(row.getCell(5)).toUpperCase().trim())
      .latitude(ExcelHelper.getNumericValue(row.getCell(6)))
      .longitude(ExcelHelper.getNumericValue(row.getCell(7)))
      .numPlatforms(ExcelHelper.getIntValue(row.getCell(8)))
      .build();
  }

  @Override
  protected List<String> validateDto(StationExcelDto dto) {
    List<String> errors = new ArrayList<>();

    // Station code
    if (dto.getStationCode() == null || dto.getStationCode().isBlank()) {
      errors.add("Station code is required");
    } else if (!dto.getStationCode().matches("^[A-Z0-9]{2,7}$")) {
      errors.add("Station code '" + dto.getStationCode() + "' must be 2-7 alphanumeric characters");
    }

    // Station name
    if (dto.getStationName() == null || dto.getStationName().isBlank()) {
      errors.add("Station name is required");
    }

    // City and state
    if (dto.getCityName() == null || dto.getCityName().isBlank()) {
      errors.add("City name is required");
    }
    if (dto.getStateName() == null || dto.getStateName().isBlank()) {
      errors.add("State name is required");
    }

    // Zone
    if (dto.getZoneName() == null || dto.getZoneName().isBlank()) {
      errors.add("Zone name is required");
    }

    // Station type enum
    if (dto.getStationType() == null || dto.getStationType().isBlank()) {
      errors.add("Station type is required");
    } else {
      try {
        StationType.valueOf(dto.getStationType());
      } catch (IllegalArgumentException e) {
        errors.add("Invalid station type '" + dto.getStationType() +
          "'. Valid values: " + List.of(StationType.values()));
      }
    }

    // Platforms
    if (dto.getNumPlatforms() == null || dto.getNumPlatforms() < 1) {
      errors.add("Number of platforms must be at least 1");
    }

    return errors;
  }

  @Override
  protected List<String> validateDtoWithDatabase(StationExcelDto dto) {
    List<String> errors = new ArrayList<>();

    // Check duplicate code — uses in-memory cache, no DB call
    if (existingCodes.contains(dto.getStationCode())) {
      errors.add("Station code '" + dto.getStationCode() + "' already exists in database");
      return errors;
    }

    // Lookup city by name + state — uses cache
    String cityKey = dto.getCityName().toLowerCase() + "|" + dto.getStateName().toLowerCase();
    CityEntity city = cityCache.get(cityKey);
    if (city == null) {
      errors.add("City '" + dto.getCityName() + "' in state '" + dto.getStateName() + "' not found");
      return errors;
    }

    if (!city.isCurrentlyActive()) {
      errors.add("City '" + dto.getCityName() + "' is inactive");
      return errors;
    }

    // Lookup zone — uses cache
    ZoneEntity zone = zoneCache.get(dto.getZoneName().toLowerCase());
    if (zone == null) {
      errors.add("Zone '" + dto.getZoneName() + "' not found");
      return errors;
    }

    return errors;
  }

  @Override
  protected List<StationEntity> saveBatch(List<StationExcelDto> dtos) {
    List<StationEntity> entities = dtos.stream()
      .map(dto -> {
        String cityKey = dto.getCityName().toLowerCase() + "|" + dto.getStateName().toLowerCase();
        CityEntity city = cityCache.get(cityKey);
        ZoneEntity zone = zoneCache.get(dto.getZoneName().toLowerCase());

        return StationEntity.builder()
          .stationCode(dto.getStationCode())
          .stationName(dto.getStationName())
          .city(city)
          .zone(zone)
          .stationType(StationType.valueOf(dto.getStationType()))
          .latitude(dto.getLatitude())
          .longitude(dto.getLongitude())
          .numPlatforms(dto.getNumPlatforms())
          .build();
      })
      .toList();

    return stationRepository.saveAll(entities);
  }

  private Boolean parseBoolean(String value) {
    if (value == null || value.isBlank()) return true;
    return value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true")
      || value.equals("1") || value.equalsIgnoreCase("active");
  }
}
