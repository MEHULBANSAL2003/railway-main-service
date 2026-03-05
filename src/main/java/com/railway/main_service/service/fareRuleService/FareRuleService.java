package com.railway.main_service.service.fareRuleService;

import com.railway.main_service.dto.request.fareRule.AddFareRuleRequest;
import com.railway.main_service.dto.response.fareRule.FareRuleResponse;

import java.time.LocalDate;
import java.util.List;

public interface FareRuleService {
  FareRuleResponse addFareRule(AddFareRuleRequest request);
  FareRuleResponse toggleStatus(Long ruleId, boolean isActive);
  List<FareRuleResponse> getAllForAdmin(String trainTypeCode, String coachTypeCode);
  List<FareRuleResponse> getComboHistory(String trainTypeCode, String coachTypeCode);
  FareRuleResponse getCurrentRule(String trainTypeCode, String coachTypeCode, LocalDate date);
}
