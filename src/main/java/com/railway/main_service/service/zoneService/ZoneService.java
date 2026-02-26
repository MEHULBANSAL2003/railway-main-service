package com.railway.main_service.service.zoneService;

import com.railway.main_service.dto.request.zone.AddZoneRequest;
import com.railway.main_service.dto.response.zone.ZoneResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ZoneService {

  ZoneResponse addNewZone(AddZoneRequest request);

  List<ZoneResponse> getAllZones(String searchTerm);
}
