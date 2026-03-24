package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.common.ChangeStatusRequest;
import com.railway.main_service.dto.request.fareRule.AddFareRuleRequest;
import com.railway.main_service.dto.response.fareRule.FareRuleResponse;
import com.railway.main_service.service.fareRuleService.FareRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.FARE_RULES)
@RequiredArgsConstructor
public class FareRuleController {

  private final FareRuleService fareRuleService;

  @PostMapping(ApiConstants.ADD_FARE_RULE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<FareRuleResponse>> addFareRule(
    @Valid @RequestBody AddFareRuleRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      fareRuleService.addFareRule(request)));
  }

  @PostMapping(ApiConstants.FARE_RULE_STATUS)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<FareRuleResponse>> changeStatus(
    @PathVariable Long ruleId,
    @Valid @RequestBody ChangeStatusRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      fareRuleService.changeStatus(ruleId, request)));
  }

  @GetMapping(ApiConstants.GET_FARE_RULES_ADMIN)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<FareRuleResponse>>> getAllForAdmin(
    @RequestParam(required = false) String trainTypeCode,
    @RequestParam(required = false) String coachTypeCode,
    @RequestParam(required = false) String quotaCode
  ) {
    return ResponseEntity.ok(ApiResponse.success(
      fareRuleService.getAllForAdmin(trainTypeCode, coachTypeCode, quotaCode)));
  }

  @GetMapping(ApiConstants.GET_FARE_RULE_HISTORY)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<FareRuleResponse>>> getComboHistory(
    @RequestParam String trainTypeCode,
    @RequestParam String coachTypeCode,
   @RequestParam String quotaCode
  ) {
    return ResponseEntity.ok(ApiResponse.success(
      fareRuleService.getComboHistory(trainTypeCode, coachTypeCode,quotaCode)));
  }

  @GetMapping(ApiConstants.GET_CURRENT_FARE_RULE)
  public ResponseEntity<ApiResponse<FareRuleResponse>> getCurrentRule(
    @RequestParam String trainTypeCode,
    @RequestParam String coachTypeCode,
    @RequestParam String quotaCode,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(ApiResponse.success(
      fareRuleService.getCurrentRule(trainTypeCode, coachTypeCode, quotaCode, date)));
  }
}
