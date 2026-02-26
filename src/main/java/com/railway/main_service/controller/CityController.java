package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.common.logging.Loggable;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.Pagination.PageRequestDto;
import com.railway.main_service.dto.request.city.AddCityRequest;
import com.railway.main_service.dto.response.city.AddCityResponse;
import com.railway.main_service.dto.response.city.CityResponse;
import com.railway.main_service.dto.response.pagination.PageResponseDto;
import com.railway.main_service.entity.CityEntity;
import com.railway.main_service.service.cityService.CityService;
import com.railway.main_service.utility.excel.ExcelUploadResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.CITIES)
@RequiredArgsConstructor
@Loggable
public class CityController {

  private final CityService cityService;

  @PostMapping(ApiConstants.ADD_CITY)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<AddCityResponse>> addCity(@Valid @RequestBody AddCityRequest request) {
    AddCityResponse response = cityService.addCity(request);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PostMapping(ApiConstants.UPLOAD_CITIES_EXCEL)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<ExcelUploadResult<CityEntity>>> uploadCitiesExcel(
    @RequestParam("file") MultipartFile file) {

    ExcelUploadResult<CityEntity> result = cityService.uploadCitiesExcel(file);
    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @GetMapping(ApiConstants.GET_CITIES)
  public ResponseEntity<PageResponseDto<CityResponse>> getAllCities(
    @RequestParam(required = false) String searchTerm,
    @ModelAttribute PageRequestDto pageRequest) {

    return ResponseEntity.ok(cityService.getAllCities(searchTerm, pageRequest));
  }

  // GET /api/cities/get/by/state/name?stateName=Maharashtra&searchTerm=pun&page=0&size=10
  @GetMapping(ApiConstants.CITIES_BY_STATE_NAME)
  public ResponseEntity<PageResponseDto<CityResponse>> getCitiesByStateName(
    @RequestParam String stateName,
    @RequestParam(required = false) String searchTerm,
    @ModelAttribute PageRequestDto pageRequest) {

    return ResponseEntity.ok(cityService.getCitiesByStateName(stateName, searchTerm, pageRequest));
  }
}
