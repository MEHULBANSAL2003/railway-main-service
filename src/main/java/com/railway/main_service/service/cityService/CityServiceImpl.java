package com.railway.main_service.service.cityService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.main_service.dto.request.Pagination.PageRequestDto;
import com.railway.main_service.dto.request.city.AddCityRequest;
import com.railway.main_service.dto.response.city.AddCityResponse;
import com.railway.main_service.dto.response.city.CityResponse;
import com.railway.main_service.dto.response.pagination.PageResponseDto;
import com.railway.main_service.entity.CityEntity;
import com.railway.main_service.entity.StateEntity;
import com.railway.main_service.mapper.CityMapper;
import com.railway.main_service.repository.CityRepository;
import com.railway.main_service.repository.StateRepository;
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

@Service
@Loggable
@Slf4j
@RequiredArgsConstructor
public class CityServiceImpl implements CityService{

  private final CityRepository cityRepository;
  private final StateRepository stateRepository;
  private final CityExcelProcessor cityExcelProcessor;

  @Override
  @Transactional
  public AddCityResponse addCity(AddCityRequest request) {
    log.info("Adding new city: {} in state: {}", request.getCityName(), request.getStateName());

    // Step 1: Trim and validate inputs
    String cityName = request.getCityName().trim();
    String stateName = request.getStateName().trim();

    StateEntity state = stateRepository.findByName(stateName)
      .orElseThrow(() -> new BaseException(
        HttpStatus.NOT_FOUND,
        "STATE_NOT_FOUND",
        "State '" + stateName + "' not found. Please check the state name."
      ));

    // Step 3: Check if state is active
    if (!state.getIsActive()) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST,
        "STATE_INACTIVE",
        "Cannot add city to inactive state '" + stateName + "'"
      );
    }

    if (cityRepository.existsByNameIgnoreCaseAndStateId(cityName, state.getId())) {
      throw new BaseException(
        HttpStatus.CONFLICT,
        "CITY_ALREADY_EXISTS",
        "City '" + cityName + "' already exists in state '" + stateName + "'"
      );
    }

    if (!cityName.matches("^[A-Za-z\\s]+$")) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST,
        "INVALID_CITY_NAME",
        "City name must contain only letters and spaces"
      );
    }

    CityEntity city = CityEntity.builder()
      .name(cityName)
      .state(state)
      .isActive(true)
      .build();

    CityEntity savedCity = cityRepository.save(city);

    log.info("Successfully created city: {} (ID: {}) in state: {} (Code: {})",
      savedCity.getName(), savedCity.getId(), state.getName(), state.getCode());

    return CityMapper.toAddCityResponse(savedCity);
  }

  @Override
  public ExcelUploadResult<CityEntity> uploadCitiesExcel(MultipartFile file) {
    log.info("Starting Excel upload for cities. File: {}, Size: {} bytes",
      file.getOriginalFilename(), file.getSize());

    ExcelUploadResult<CityEntity> result = cityExcelProcessor.processExcelFile(file);

    log.info("Excel upload completed. Success: {}, Failed: {}, Total: {}",
      result.getSuccessCount(), result.getFailureCount(), result.getTotalRows());

    return result;
  }

  @Override
  public PageResponseDto<CityResponse> getAllCities(String searchTerm, PageRequestDto pageRequest) {
    String search = resolveSearchTerm(searchTerm);
    Pageable pageable = PaginationUtils.createPageable(pageRequest);

    log.info("Fetching all cities | searchTerm='{}' | page={} size={}",
      search, pageRequest.getPage(), pageRequest.getSize());

    Page<CityEntity> page = cityRepository.findAllWithSearch(search, pageable);
    return PaginationUtils.toPageResponse(page, CityMapper::toCityResponse);
  }

  @Override
  public PageResponseDto<CityResponse> getCitiesByStateName(String stateName, String searchTerm, PageRequestDto pageRequest) {
    if (stateName == null || stateName.trim().isEmpty()) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "MISSING_STATE_NAME", "State name is required");
    }

    String trimmedStateName = stateName.trim();
    stateRepository.findByName(trimmedStateName)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "STATE_NOT_FOUND",
        "No state found with name: " + trimmedStateName));

    String search = resolveSearchTerm(searchTerm);
    Pageable pageable = PaginationUtils.createPageable(pageRequest);

    log.info("Fetching cities for state='{}' | searchTerm='{}' | page={} size={}",
      trimmedStateName, search, pageRequest.getPage(), pageRequest.getSize());

    Page<CityEntity> page = cityRepository.findByStateNameWithSearch(trimmedStateName, search, pageable);
    return PaginationUtils.toPageResponse(page, CityMapper::toCityResponse);
  }

  // Converts blank/null searchTerm to null so JPQL "IS NULL" check bypasses the filter
  private String resolveSearchTerm(String searchTerm) {
    return (searchTerm != null && !searchTerm.trim().isEmpty()) ? searchTerm.trim() : null;
  }

}
