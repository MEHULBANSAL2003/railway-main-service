package com.railway.main_service.service.fareRuleService;

import com.railway.main_service.dto.request.common.ChangeStatusRequest;
import com.railway.main_service.dto.request.fareRule.AddFareRuleRequest;
import com.railway.main_service.dto.response.fareRule.FareRuleResponse;

import java.time.LocalDate;
import java.util.List;

public interface FareRuleService {
  FareRuleResponse addFareRule(AddFareRuleRequest request);
  FareRuleResponse changeStatus(Long ruleId, ChangeStatusRequest request);
  List<FareRuleResponse> getAllForAdmin(String trainTypeCode, String coachTypeCode, String quotaCode);

  List<FareRuleResponse> getComboHistory(String trainTypeCode, String coachTypeCode, String quotaCode);

  FareRuleResponse getCurrentRule(String trainTypeCode, String coachTypeCode, String quotaCode, LocalDate date);
}
