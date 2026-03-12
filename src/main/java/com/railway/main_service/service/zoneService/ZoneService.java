package com.railway.main_service.service.zoneService;

import com.railway.common.pagination.FilterRequest;
import com.railway.common.pagination.PagedResponse;
import com.railway.main_service.dto.request.zone.CreateZoneRequest;
import com.railway.main_service.dto.request.zone.UpdateZoneRequest;
import com.railway.main_service.dto.request.zone.ZoneStatusRequest;
import com.railway.main_service.dto.response.zone.ZoneResponse;

public interface ZoneService {
  ZoneResponse createZone(CreateZoneRequest request);
  ZoneResponse updateZone(Long zoneId, UpdateZoneRequest request);
  ZoneResponse deactivateZone(Long zoneId, ZoneStatusRequest request);
  ZoneResponse reactivateZone(Long zoneId, ZoneStatusRequest request);
  ZoneResponse getActiveZoneById(Long zoneId);
  PagedResponse<ZoneResponse> getAllZones(FilterRequest request);
  void createZoneFromExcel(CreateZoneRequest request);
}
