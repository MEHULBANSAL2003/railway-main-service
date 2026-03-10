package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.response.inventory.JourneyInventoryResponse;
import com.railway.main_service.service.inventoryService.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstants.TRAIN_JOURNEYS)
@RequiredArgsConstructor
public class InventoryController {

  private final InventoryService inventoryService;

  @GetMapping("/{journeyId}/inventory")
  public ResponseEntity<ApiResponse<JourneyInventoryResponse>> getInventory(
    @PathVariable String trainNumber,
    @PathVariable Long   journeyId) {
    return ResponseEntity.ok(
      ApiResponse.success(inventoryService.getForJourney(trainNumber, journeyId))
    );
  }
}
