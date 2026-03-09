package com.railway.main_service.service.trainStopService;

import com.railway.main_service.dto.request.trainStop.AddTrainStopRequest;
import com.railway.main_service.dto.request.trainStop.BulkAddTrainStopRequest;
import com.railway.main_service.dto.request.trainStop.UpdateTrainStopRequest;
import com.railway.main_service.dto.response.trainStop.CopyStopsPreviewResponse;
import com.railway.main_service.dto.response.trainStop.TrainStopResponse;

import java.util.List;

public interface TrainStopService {
  List<TrainStopResponse> getAllByTrain(String trainNumber);
  TrainStopResponse addStop(String trainNumber, AddTrainStopRequest request);
  TrainStopResponse updateStop(String trainNumber, Long stopId, UpdateTrainStopRequest request);
  void deleteStop(String trainNumber, Long stopId);

  List<TrainStopResponse> bulkAddStops(String trainNumber,
                                       BulkAddTrainStopRequest request);

  CopyStopsPreviewResponse getCopyPreview(String sourceTrainNumber,
                                          String targetTrainNumber);


}
