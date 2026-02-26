package com.railway.main_service.service.cityService;

import com.railway.main_service.dto.request.city.AddCityRequest;
import com.railway.main_service.dto.response.city.AddCityResponse;
import com.railway.main_service.entity.CityEntity;
import com.railway.main_service.utility.excel.ExcelUploadResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface CityService {

  AddCityResponse addCity(AddCityRequest request);

  // Upload cities via Excel
  ExcelUploadResult<CityEntity> uploadCitiesExcel(MultipartFile file);
//
//  // Get cities by state code
//  List<CityResponse> getCitiesByStateCode(String stateCode);
//
//  // Get cities by state name
//  List<CityResponse> getCitiesByStateName(String stateName);
//
//  // Search cities globally
//  List<CityResponse> searchCities(String query);
//
//  // Get city by ID
//  CityResponse getCityById(Long id);
}
