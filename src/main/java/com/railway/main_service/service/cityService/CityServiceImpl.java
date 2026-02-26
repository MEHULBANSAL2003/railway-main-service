package com.railway.main_service.service.cityService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.main_service.dto.request.city.AddCityRequest;
import com.railway.main_service.dto.response.city.AddCityResponse;
import com.railway.main_service.entity.CityEntity;
import com.railway.main_service.entity.StateEntity;
import com.railway.main_service.mapper.CityMapper;
import com.railway.main_service.repository.CityRepository;
import com.railway.main_service.repository.StateRepository;
import com.railway.main_service.utility.excel.ExcelUploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

}
