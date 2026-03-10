package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.journey.AddJourneyRequest;
import com.railway.main_service.dto.request.journey.CancelJourneyRequest;
import com.railway.main_service.dto.response.journey.BulkGenerateResponse;
import com.railway.main_service.dto.response.journey.JourneyResponse;
import com.railway.main_service.service.journeyService.JourneyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.TRAIN_JOURNEYS)
@RequiredArgsConstructor
public class JourneyController {

  private final JourneyService journeyService;

  // GET /api/main/trains/{trainNumber}/journeys
  @GetMapping
  public ResponseEntity<ApiResponse<Page<JourneyResponse>>> getAll(
    @PathVariable String trainNumber,
    @RequestParam(defaultValue = "0")            int     page,
    @RequestParam(defaultValue = "20")           int     size,
    @RequestParam(defaultValue = "journeyDate")  String  sortBy,
    @RequestParam(defaultValue = "desc")         String  sortDir,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
    @RequestParam(required = false) String statuses) {

    List<String> statusList = (statuses != null && !statuses.isBlank())
      ? List.of(statuses.split(","))
      : List.of();

    return ResponseEntity.ok(
      ApiResponse.success(
      journeyService.getJourneysForTrain(trainNumber, page, size, sortBy, sortDir, dateFrom, dateTo, statusList))
    );
  }

  // POST /api/main/trains/{trainNumber}/journeys/generate
  // Auto-generates journey for 120 days ahead (same as nightly job, scoped to one train)
  @PostMapping(ApiConstants.GENERATE_TRAIN_JOURNEY)
  public ResponseEntity<ApiResponse<JourneyResponse>> generate(
    @PathVariable String trainNumber) {
    return ResponseEntity.ok(ApiResponse.success(journeyService.generateForTrain(trainNumber)));
  }

  @PostMapping("/bulk-generate")
  public ResponseEntity<ApiResponse<BulkGenerateResponse>> bulkGenerate(@PathVariable String trainNumber) {
    return ResponseEntity.ok(ApiResponse.success(journeyService.bulkGenerate(trainNumber)));
  }

  // POST /api/main/trains/{trainNumber}/journeys/add
  // Admin manually adds a journey for a specific date
  @PostMapping(ApiConstants.ADD_TRAIN_JOURNEY)
  public ResponseEntity<ApiResponse<JourneyResponse>> add(
    @PathVariable String trainNumber,
    @Valid @RequestBody AddJourneyRequest request) {
    return ResponseEntity.ok(ApiResponse.success(journeyService.addJourney(trainNumber, request)));
  }

  // POST /api/main/trains/{trainNumber}/journeys/{journeyId}/cancel
  @PostMapping(ApiConstants.CANCEL_TRAIN_JOURNEY)
  public ResponseEntity<Void> cancel(
    @PathVariable String trainNumber,
    @PathVariable Long journeyId,
    @Valid @RequestBody CancelJourneyRequest request) {
    journeyService.cancelJourney(trainNumber, journeyId, request);
    return ResponseEntity.ok().build();
  }
}
