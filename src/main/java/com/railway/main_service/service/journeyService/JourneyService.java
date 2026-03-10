package com.railway.main_service.service.journeyService;

import com.railway.main_service.dto.request.journey.AddJourneyRequest;
import com.railway.main_service.dto.request.journey.CancelJourneyRequest;
import com.railway.main_service.dto.response.journey.BulkGenerateResponse;
import com.railway.main_service.dto.response.journey.JourneyResponse;

import java.util.List;

public interface JourneyService {

  // Auto-generate journey 120 days ahead for a specific train
  // (same logic as the nightly job but scoped to one train)
  JourneyResponse generateForTrain(String trainNumber);

  BulkGenerateResponse bulkGenerate(String trainNumber);

  // Admin manually adds a journey for a specific date
  JourneyResponse addJourney(String trainNumber, AddJourneyRequest request);

  // List all journeys for a train
  List<JourneyResponse> getJourneysForTrain(String trainNumber);

  // Cancel a specific journey
  void cancelJourney(String trainNumber, Long journeyId, CancelJourneyRequest request);
}
