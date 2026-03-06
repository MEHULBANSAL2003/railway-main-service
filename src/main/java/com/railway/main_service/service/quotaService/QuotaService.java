package com.railway.main_service.service.quotaService;

import com.railway.main_service.dto.request.quota.AddQuotaRequest;
import com.railway.main_service.dto.request.quota.UpdateQuotaRequest;
import com.railway.main_service.dto.response.quota.QuotaResponse;

import java.util.List;

public interface QuotaService {

  public QuotaResponse addQuota(AddQuotaRequest request);

  public QuotaResponse updateQuota(String quotaCode, UpdateQuotaRequest request);

  public QuotaResponse toggleStatus(String quotaCode, boolean isActive);

  public List<QuotaResponse> getAllForDropdown();

  public List<QuotaResponse> getAllForAdmin();
}
