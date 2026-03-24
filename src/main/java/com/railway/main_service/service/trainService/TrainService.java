package com.railway.main_service.service.trainService;

import com.railway.main_service.dto.request.common.ChangeStatusRequest;
import com.railway.main_service.dto.request.train.AddTrainRequest;
import com.railway.main_service.dto.request.train.UpdateTrainRequest;
import com.railway.main_service.dto.response.PageResponse;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.train.BulkUploadResponse;
import com.railway.main_service.dto.response.train.ReturnTrainResponse;
import com.railway.main_service.dto.response.train.TrainResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TrainService {

  TrainResponse addTrain(AddTrainRequest request);

  TrainResponse updateTrain(String trainNumber, UpdateTrainRequest request);

  TrainResponse changeStatus(String trainNumber, ChangeStatusRequest request);

  CascadeInfoResponse getCascadeInfo(String trainNumber);

  // Paginated + filtered + sorted admin query
  // page is 1-based. sortBy: trainNumber | trainName | isActive | pantrycar
  // sortDir: asc | desc
  PageResponse<TrainResponse> getAllForAdmin(
    String search,
    String trainTypeCode,
    String zoneCode,
    Boolean isActive,
    int page,
    int size,
    String sortBy,
    String sortDir
  );

  List<TrainResponse> getAllForDropdown(String search);

  // Return train pairing — used by "Add return train?" prompt
  ReturnTrainResponse getReturnTrainInfo(String trainNumber);

  // Excel bulk upload
  BulkUploadResponse uploadFromExcel(MultipartFile file);

  TrainResponse getTrainDetails(String trainNumber);

  // Download blank Excel template
  byte[] getExcelTemplate();
}
