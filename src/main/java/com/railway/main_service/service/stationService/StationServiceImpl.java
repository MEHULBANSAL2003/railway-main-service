package com.railway.main_service.service.stationService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.station.AddNewStationRequest;
import com.railway.main_service.dto.request.station.DeleteStationRequest;
import com.railway.main_service.dto.request.station.StationFilterRequest;
import com.railway.main_service.dto.request.station.UpdateStationRequest;
import com.railway.main_service.dto.response.pagination.PageResponseDto;
import com.railway.main_service.dto.response.station.AddNewStationResponse;
import com.railway.main_service.dto.response.station.DeleteStationResponse;
import com.railway.main_service.dto.response.station.RestoreDeletedStationResponse;
import com.railway.main_service.dto.response.station.StationResponse;
import com.railway.main_service.entity.CityEntity;
import com.railway.main_service.entity.StationEntity;
import com.railway.main_service.entity.ZoneEntity;
import com.railway.main_service.mapper.StationMapper;
import com.railway.main_service.repository.CityRepository;
import com.railway.main_service.repository.StateRepository;
import com.railway.main_service.repository.StationRepository;
import com.railway.main_service.repository.ZoneRepository;
import com.railway.main_service.specification.StationSpecification;
import com.railway.main_service.utility.Pagination.PaginationUtils;
import com.railway.main_service.utility.excel.ExcelUploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Loggable
@Slf4j
@RequiredArgsConstructor
public class StationServiceImpl implements StationService{

  private final StationRepository stationRepository;
  private final StationExcelProcessor stationExcelProcessor;
  private final CityRepository cityRepository;
  private final ZoneRepository zoneRepository;

  private static final Set<String> STATION_SORT_FIELDS = Set.of(
    "id", "stationCode", "stationName", "stationType",
    "numPlatforms", "isActive", "createdAt", "updatedAt", "cityName"
  );

  @Override
  @Transactional
  public AddNewStationResponse addNewStation(AddNewStationRequest request) {

    String stationCode = request.getStationCode().trim().toUpperCase();

    if(stationRepository.existsByStationNameOrStationCode(request.getStationName().trim(), stationCode)){
      throw new BaseException(HttpStatus.CONFLICT, "STATION_NAME_EXISTS",
        "Station with name '" + request.getStationName() + "' already exists. Needed Super Admin access to activate that (if permanently deleted)");
    }

    CityEntity city = cityRepository.findById(request.getCityId())
      .orElseThrow(() -> new BaseException(
        HttpStatus.NOT_FOUND,
        "CITY_NOT_FOUND",
        "City not found with id: " + request.getCityId()
      ));

    if(!city.getState().getIsActive() || !city.getIsActive()){
        throw new BaseException(
        HttpStatus.BAD_REQUEST,
        "CITY_STATE_INACTIVE",
        "City or state is inactive");
    }

    if(!city.getState().getId().equals(request.getStateId())){
      throw new BaseException(
        HttpStatus.BAD_REQUEST,
        "CITY_STATE_MISMATCH",
        "City state does not match with state id: " + request.getStateId()
      );
    }

    ZoneEntity zone = zoneRepository.findById(request.getZoneId())
      .orElseThrow(() -> new BaseException(
        HttpStatus.NOT_FOUND,
        "ZONE_NOT_FOUND",
        "Zone not found with id: " + request.getZoneId()
      ));

    StationEntity station = StationEntity.builder()
      .stationCode(stationCode)
      .stationName(request.getStationName().trim())
      .city(city)
      .zone(zone)
      .stationType(request.getStationType())
      .numPlatforms(request.getNumPlatforms())
      .latitude(request.getLatitude())
      .longitude(request.getLongitude())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    StationEntity saved = stationRepository.save(station);

    return AddNewStationResponse.builder()
      .stationId(saved.getId())
      .stationCode(saved.getStationCode())
      .stationName(saved.getStationName())
      .cityName(saved.getCity().getName())
      .stateName(saved.getCity().getState().getName())
      .zoneName(saved.getZone().getName())
      .stationType(saved.getStationType())
      .numPlatforms(saved.getNumPlatforms())
      .createdBy(saved.getCreatedBy())
      .createdAt(saved.getCreatedAt())
      .isActive(saved.getIsActive())
      .message("Station created successfully")
      .build();
  }


  @Override
  @Transactional(readOnly = true)
  public PageResponseDto<StationResponse> getAllStations(StationFilterRequest filter) {

    // ── Validate sort field ──────────────────────────────────────────────────
    String sortBy = STATION_SORT_FIELDS.contains(filter.getSortBy())
      ? filter.getSortBy()
      : "stationId";

    Sort.Direction direction = "DESC".equalsIgnoreCase(filter.getSortDirection())
      ? Sort.Direction.DESC
      : Sort.Direction.ASC;

    Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(),
      Sort.by(direction, sortBy));

    // ── Fast path — no filters, use optimised fetch-join query ───────────────
    boolean hasFilters = hasValue(filter.getSearchTerm())
      || hasValue(filter.getState())
      || hasValue(filter.getZone())
      || hasValue(filter.getStationType())
      || filter.getIsActive() != null;

    Page<StationEntity> page;
    if (!hasFilters) {
      page = stationRepository.findAllWithDetails(pageable);
    } else {
      // Specification path — handles all filter combinations
      Specification<StationEntity> spec = StationSpecification.build(filter);
      page = stationRepository.findAll(spec, pageable);
    }

    return PaginationUtils.toPageResponse(page, StationMapper::toDto);
  }

  @Override
  public PageResponseDto<StationResponse> getAllPermanentlyDeletedStations(StationFilterRequest filter) {
    String sortBy = STATION_SORT_FIELDS.contains(filter.getSortBy())
      ? filter.getSortBy()
      : "stationId";

    Sort.Direction direction = "DESC".equalsIgnoreCase(filter.getSortDirection())
      ? Sort.Direction.DESC
      : Sort.Direction.ASC;

    Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(),
      Sort.by(direction, sortBy));

    // ── Fast path — no filters, use optimised fetch-join query ───────────────
    boolean hasFilters = hasValue(filter.getSearchTerm())
      || hasValue(filter.getState())
      || hasValue(filter.getZone())
      || hasValue(filter.getStationType())
      || filter.getIsActive() != null;

    Page<StationEntity> page;
    if (!hasFilters) {
      page = stationRepository.findAllPermanentlyDeletedWithDetails(pageable);
    } else {
      // Specification path — handles all filter combinations
      Specification<StationEntity> spec = StationSpecification.buildDeleted(filter);
      page = stationRepository.findAll(spec, pageable);
    }

    return PaginationUtils.toPageResponse(page, StationMapper::toDto);
  }

  private boolean hasValue(String s) {
    return s != null && !s.isBlank();
  }

  @Override
  public ExcelUploadResult uploadStationsExcel(MultipartFile file) {
    log.info("Starting Excel upload for stations. File: {}, Size: {} bytes",
      file.getOriginalFilename(), file.getSize());

    ExcelUploadResult result = stationExcelProcessor.processExcelFile(file);

    log.info("Excel upload completed. Success: {}, Failed: {}, Total: {}",
      result.getSuccessCount(), result.getFailureCount(), result.getTotalRows());

    return result;
  }



  @Override
@Transactional
  public StationResponse updateActiveInactiveStatus(String stationCode, boolean isActive) {
    StationEntity station = stationRepository
      .findByStationCode(stationCode)
      .orElseThrow(() -> new BaseException(
        HttpStatus.NOT_FOUND,
        "STATION_NOT_FOUND",
        "Station not found with code: " + stationCode
      ));

    if (station.getIsActive().equals(isActive)) {
      return StationMapper.toDto(station);
    }

    station.setIsActive(isActive);
    station.setUpdatedAt(java.time.LocalDateTime.now());
    station.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    if(!isActive){
      station.setDeletedAt(java.time.LocalDateTime.now());
      station.setDeletedBy(SecurityUtils.getCurrentAdminId());
    }
    else{
      station.setDeletedAt(null);
      station.setDeletedBy(null);
    }
    stationRepository.save(station);

    return StationMapper.toDto(station);

  }

  @Override
  @Transactional
  public AddNewStationResponse updateStationDetails(String stationCode, UpdateStationRequest request) {

    // ── 1. Fetch station ─────────────────────────────────────────────────────
    StationEntity station = stationRepository.findByStationCode(stationCode)
      .orElseThrow(() -> new BaseException(
        HttpStatus.NOT_FOUND,
        "STATION_NOT_FOUND",
        "Station not found with code: " + stationCode
      ));

    // ── 2. Station must be active to be updated ──────────────────────────────
    if (!station.getIsActive()) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST,
        "STATION_INACTIVE",
        "Cannot update an inactive station. Activate it first."
      );
    }

    // ── 3. cityId and stateId must be provided together or not at all ────────
    boolean hasCityId  = request.getCityId()  != null;
    boolean hasStateId = request.getStateId() != null;

    if (hasCityId != hasStateId) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST,
        "CITY_STATE_REQUIRED_TOGETHER",
        "cityId and stateId must both be provided together, or both omitted."
      );
    }

    // ── 4. Validate and apply city + state ───────────────────────────────────
    if (hasCityId) {
      CityEntity city = cityRepository.findById(request.getCityId())
        .orElseThrow(() -> new BaseException(
          HttpStatus.NOT_FOUND,
          "CITY_NOT_FOUND",
          "City not found with id: " + request.getCityId()
        ));

      // City must be active
      if (!city.getIsActive()) {
        throw new BaseException(
          HttpStatus.BAD_REQUEST,
          "CITY_INACTIVE",
          "City with id " + request.getCityId() + " is inactive."
        );
      }

      // State must be active
      if (!city.getState().getIsActive()) {
        throw new BaseException(
          HttpStatus.BAD_REQUEST,
          "STATE_INACTIVE",
          "State associated with city " + city.getName() + " is inactive."
        );
      }

      // City must belong to the provided state
      if (!city.getState().getId().equals(request.getStateId())) {
        throw new BaseException(
          HttpStatus.BAD_REQUEST,
          "CITY_STATE_MISMATCH",
          "City '" + city.getName() + "' does not belong to state id: " + request.getStateId()
        );
      }

      station.setCity(city);
    }

    // ── 5. Validate and apply zone ───────────────────────────────────────────
    if (request.getZoneId() != null) {
      ZoneEntity zone = zoneRepository.findById(request.getZoneId())
        .orElseThrow(() -> new BaseException(
          HttpStatus.NOT_FOUND,
          "ZONE_NOT_FOUND",
          "Zone not found with id: " + request.getZoneId()
        ));

      // Optional: check zone is active if your ZoneEntity has isActive
       if (!zone.getIsActive()) {
         throw new BaseException(HttpStatus.BAD_REQUEST, "ZONE_INACTIVE", "Zone is inactive.");
       }

      station.setZone(zone);
    }

    // ── 6. Apply scalar field updates (only if provided) ────────────────────
    if (request.getStationName() != null && !request.getStationName().isBlank()) {
      // Check for duplicate name (exclude current station)
      boolean nameConflict = stationRepository.existsByStationNameOrStationCode(request.getStationName().trim(), stationCode);
      if (nameConflict) {
        throw new BaseException(
          HttpStatus.CONFLICT,
          "STATION_NAME_EXISTS",
          "Another station with name '" + request.getStationName() + "' already exists."
        );
      }

      if(stationRepository.existsByStationName(request.getStationName())){
        throw new BaseException(HttpStatus.CONFLICT, "STATION_NAME_EXISTS",
          "Station with name '" + request.getStationName() + "' already exists. Needed Super Admin access to activate that");
      }
      station.setStationName(request.getStationName().trim());
    }

    if (request.getStationType() != null) {
      station.setStationType(request.getStationType());
    }

    if (request.getNumPlatforms() != null) {
      station.setNumPlatforms(request.getNumPlatforms());
    }
    station.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    station.setUpdatedAt(java.time.LocalDateTime.now());

    // ── 7. Save and return ───────────────────────────────────────────────────
    StationEntity saved = stationRepository.save(station);

    return AddNewStationResponse.builder()
      .stationId(saved.getId())
      .stationCode(saved.getStationCode())
      .stationName(saved.getStationName())
      .cityName(saved.getCity().getName())
      .stateName(saved.getCity().getState().getName())
      .zoneName(saved.getZone().getName())
      .stationType(saved.getStationType())
      .numPlatforms(saved.getNumPlatforms())
      .createdBy(saved.getCreatedBy())
      .createdAt(saved.getCreatedAt())
      .updatedAt(saved.getUpdatedAt())
      .updatedBy(saved.getUpdatedBy())
      .isActive(saved.getIsActive())
      .message("Station updated successfully")
      .build();
  }

  @Override
  @Transactional
  public DeleteStationResponse deleteStation(String stationCode, DeleteStationRequest request) {

    StationEntity station = stationRepository.findByStationCodeIncludeDeleted(stationCode)
      .orElseThrow(() -> new BaseException(
        HttpStatus.NOT_FOUND,
        "STATION_NOT_FOUND",
        "Station not found with code: " + stationCode
      ));

    if(station.getIsActive()){
      throw new BaseException(
        HttpStatus.BAD_REQUEST,
        "STATION_ACTIVE",
        "Cannot delete an active station. Deactivate it first."
      );
    }

    if (station.getIsPermanentlyDeleted()) {
      throw new BaseException(
        HttpStatus.GONE,
        "STATION_ALREADY_DELETED",
        "Station '" + stationCode + "' has already been deleted."
      );
    }

    station.setIsPermanentlyDeleted(true);
    station.setIsActive(false);
    station.setDeletedAt(java.time.LocalDateTime.now());
    station.setDeletedBy(SecurityUtils.getCurrentAdminId());
    station.setPermanentDeleteReason(request.getReason().trim());

    stationRepository.save(station);

    return DeleteStationResponse.builder()
      .message("Station '" + station.getStationName() + "' (" + stationCode + ") deleted successfully.")
      .build();
  }

  @Override
  public RestoreDeletedStationResponse restoreDeletedStation(String stationCode) {

    StationEntity station = stationRepository.findByStationCodeIncludeDeleted(stationCode)
      .orElseThrow(() -> new BaseException(
        HttpStatus.NOT_FOUND, "STATION_NOT_FOUND",
        "Station not found with code: " + stationCode
      ));

    if (!station.getIsPermanentlyDeleted()) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST, "STATION_NOT_DELETED",
        "Station '" + stationCode + "' is not deleted. Nothing to restore."
      );
    }

    if (stationRepository.existsByStationNameAndIsPermanentlyDeletedFalse(station.getStationName())) {
      throw new BaseException(
        HttpStatus.CONFLICT, "STATION_NAME_TAKEN",
        "Cannot restore — another active station with name '" + station.getStationName() + "' now exists."
      );
    }

    station.setIsPermanentlyDeleted(false);
    station.setIsActive(true);
    station.setDeletedAt(null);
    station.setPermanentDeleteReason(null);
    station.setDeletedBy(null);
    station.setUpdatedAt(LocalDateTime.now());
    station.setUpdatedBy(SecurityUtils.getCurrentAdminId());
   stationRepository.save(station);

    return RestoreDeletedStationResponse.builder()
      .message("Station '" + station.getStationName() + "' (" + stationCode + ") restored successfully.")
      .build();

  }
}
