package com.railway.main_service.service.stationService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.Pagination.PageRequestDto;
import com.railway.main_service.dto.request.station.AddNewStationRequest;
import com.railway.main_service.dto.response.pagination.PageResponseDto;
import com.railway.main_service.dto.response.station.AddNewStationResponse;
import com.railway.main_service.dto.response.station.StationResponse;
import com.railway.main_service.entity.StationEntity;
import com.railway.main_service.mapper.StationMapper;
import com.railway.main_service.repository.StationRepository;
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
public class StationServiceImpl implements StationService{

  private final StationRepository stationRepository;
  private final StationExcelProcessor stationExcelProcessor;

  @Override
  @Transactional
  public AddNewStationResponse addNewStation(AddNewStationRequest request) {

     boolean isStationCodeExists = stationRepository.existsByStationCode(request.getStationCode());

     if (isStationCodeExists){
       throw new BaseException(HttpStatus.CONFLICT,"STATION_ALREADY_EXISTS","Station already exists");
     }

    StationEntity station = StationEntity.builder()
      .stationCode(request.getStationCode())  // Fixed field names
      .stationName(request.getStationName())
      .city(request.getCity())
      .state(request.getState())
      .zone(request.getZone())
      .numPlatforms(request.getNumPlatforms())
      .isJunction(request.isJunction())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    StationEntity savedStation = stationRepository.save(station);

    return AddNewStationResponse.builder()
      .stationId(savedStation.getId())
      .stationCode(savedStation.getStationCode())
      .stationName(savedStation.getStationName())
      .city(savedStation.getCity())
      .state(savedStation.getState())
      .zone(savedStation.getZone())
      .numPlatforms(savedStation.getNumPlatforms())
      .isJunction(savedStation.isJunction())
      .createdBy(savedStation.getCreatedBy())
      .createdAt(savedStation.getCreatedAt())
      .message("Station created successfully")
      .build();
  }


  @Override
  @Transactional(readOnly = true)
  public PageResponseDto<StationResponse> getAllStations(PageRequestDto pageRequest) {

    Pageable pageable = PaginationUtils.createPageable(pageRequest);

    Page<StationEntity> stationPage = stationRepository.findAll(pageable);

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
}
