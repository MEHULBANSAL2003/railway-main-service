package com.railway.main_service.service.journeyService;

import com.railway.main_service.dto.request.journey.AddJourneyRequest;
import com.railway.main_service.dto.request.journey.CancelJourneyRequest;
import com.railway.main_service.dto.response.journey.BulkGenerateResponse;
import com.railway.main_service.dto.response.journey.JourneyResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface JourneyService {

  Page<JourneyResponse> getJourneysForTrain(
    String trainNumber, int page, int size,
    String sortBy, String sortDir,
    LocalDate dateFrom, LocalDate dateTo,
    List<String> statuses);

  BulkGenerateResponse bulkGenerate(String trainNumber);

  JourneyResponse generateForTrain(String trainNumber);

  JourneyResponse addJourney(String trainNumber, AddJourneyRequest request);

  void cancelJourney(String trainNumber, Long journeyId, CancelJourneyRequest request);
}
