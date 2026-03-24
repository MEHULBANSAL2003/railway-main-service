package com.railway.main_service.service.stationService;

import com.railway.main_service.dto.request.common.ChangeStatusRequest;
import com.railway.main_service.dto.request.Pagination.PageRequestDto;
import com.railway.main_service.dto.request.station.AddNewStationRequest;
import com.railway.main_service.dto.request.station.DeleteStationRequest;
import com.railway.main_service.dto.request.station.StationFilterRequest;
import com.railway.main_service.dto.request.station.UpdateStationRequest;
import com.railway.main_service.dto.response.pagination.PageResponseDto;
import com.railway.main_service.dto.response.station.AddNewStationResponse;
import com.railway.main_service.dto.response.station.DeleteStationResponse;
import com.railway.main_service.dto.response.station.RestoreDeletedStationResponse;
import com.railway.main_service.dto.response.station.StationResponse;
import com.railway.main_service.utility.excel.ExcelUploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StationService {

  AddNewStationResponse addNewStation(AddNewStationRequest request);

  PageResponseDto<StationResponse> getAllStations(StationFilterRequest pageRequest);
  PageResponseDto<StationResponse> getAllPermanentlyDeletedStations(StationFilterRequest pageRequest);

  List<StationResponse> getAllStationsForDropdown(String searchTerm);


  ExcelUploadResult uploadStationsExcel(MultipartFile file);


  StationResponse changeStatus(String stationCode, ChangeStatusRequest request);

  AddNewStationResponse updateStationDetails(String stationCode, UpdateStationRequest request);

  DeleteStationResponse deleteStation(String stationCode, DeleteStationRequest request);

  RestoreDeletedStationResponse restoreDeletedStation(String stationCode);


}
