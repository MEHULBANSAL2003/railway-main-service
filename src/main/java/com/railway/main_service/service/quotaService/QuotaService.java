package com.railway.main_service.service.quotaService;

import com.railway.main_service.dto.request.common.ChangeStatusRequest;
import com.railway.main_service.dto.request.quota.AddQuotaRequest;
import com.railway.main_service.dto.request.quota.UpdateQuotaRequest;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.quota.QuotaResponse;

import java.util.List;

public interface QuotaService {

  public QuotaResponse addQuota(AddQuotaRequest request);

  public QuotaResponse updateQuota(String quotaCode, UpdateQuotaRequest request);

  public QuotaResponse changeStatus(String quotaCode, ChangeStatusRequest request);

  public List<QuotaResponse> getAllForDropdown();

  public List<QuotaResponse> getAllForAdmin();
  public CascadeInfoResponse getCascadeInfo(String quotaCode);
}
