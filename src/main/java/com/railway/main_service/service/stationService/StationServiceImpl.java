package com.railway.main_service.service.stationService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.Pagination.PageRequestDto;
import com.railway.main_service.dto.request.station.AddNewStationRequest;
import com.railway.main_service.dto.response.pagination.PageResponseDto;
import com.railway.main_service.dto.response.station.AddNewStationResponse;
import com.railway.main_service.dto.response.station.StationResponse;
import com.railway.main_service.entity.CityEntity;
import com.railway.main_service.entity.StationEntity;
import com.railway.main_service.entity.ZoneEntity;
import com.railway.main_service.mapper.StationMapper;
import com.railway.main_service.repository.CityRepository;
import com.railway.main_service.repository.StateRepository;
import com.railway.main_service.repository.StationRepository;
import com.railway.main_service.repository.ZoneRepository;
import com.railway.main_service.utility.Pagination.PaginationUtils;
import com.railway.main_service.utility.excel.ExcelUploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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

  @Override
  @Transactional
  public AddNewStationResponse addNewStation(AddNewStationRequest request) {

    String stationCode = request.getStationCode().trim().toUpperCase();
    if (stationRepository.existsByStationCode(stationCode)) {
      throw new BaseException(
        HttpStatus.CONFLICT,
        "STATION_ALREADY_EXISTS",
        "Station with code '" + stationCode + "' already exists"
      );
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
      .message("Station created successfully")
      .build();
  }


  @Override
  @Transactional(readOnly = true)
  public PageResponseDto<StationResponse> getAllStations(PageRequestDto pageRequest) {
    Pageable pageable = PaginationUtils.createPageable(pageRequest, PaginationUtils.STATION_SORT_FIELDS);
    Page<StationEntity> stationPage = stationRepository.findAllWithDetails(pageable);
    return PaginationUtils.toPageResponse(stationPage, StationMapper::toDto);
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
  @Transactional(readOnly = true)
  public List<StationResponse> searchStations(String searchTerm) {
    String term = searchTerm.trim();
    log.info("Searching stations with term: '{}'", term);

    List<StationEntity> stations = stationRepository.searchStations(term);

    log.info("Found {} stations matching: '{}'", stations.size(), term);

    return stations.stream()
      .map(StationMapper::toDto)
      .collect(Collectors.toList());
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
    stationRepository.save(station);

    return StationMapper.toDto(station);

  }
}
