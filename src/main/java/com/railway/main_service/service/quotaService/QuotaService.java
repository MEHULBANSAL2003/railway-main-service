package com.railway.main_service.service.quotaService;

import com.railway.main_service.dto.request.ActivateRequest;
import com.railway.main_service.dto.request.DeactivateRequest;
import com.railway.main_service.dto.request.quota.AddQuotaRequest;
import com.railway.main_service.dto.request.quota.UpdateQuotaRequest;
import com.railway.main_service.dto.response.DeactivationResponse;
import com.railway.main_service.dto.response.PeriodResponse;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.quota.QuotaResponse;

import java.util.List;

public interface QuotaService {

  QuotaResponse addQuota(AddQuotaRequest request);

  QuotaResponse updateQuota(String quotaCode, UpdateQuotaRequest request);

  DeactivationResponse deactivate(String quotaCode, DeactivateRequest request);

  DeactivationResponse activate(String quotaCode, ActivateRequest request);

  List<PeriodResponse> getPeriods(String quotaCode);

  List<QuotaResponse> getAllForDropdown();

  List<QuotaResponse> getAllForAdmin();

  CascadeInfoResponse getCascadeInfo(String quotaCode);
}
