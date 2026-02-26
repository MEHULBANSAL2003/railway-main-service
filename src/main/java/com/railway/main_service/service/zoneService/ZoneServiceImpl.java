package com.railway.main_service.service.zoneService;

import com.railway.common.exceptions.BaseException;
import com.railway.main_service.dto.request.zone.AddZoneRequest;
import com.railway.main_service.dto.response.zone.ZoneResponse;
import com.railway.main_service.entity.ZoneEntity;
import com.railway.main_service.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZoneServiceImpl implements ZoneService {

  private final ZoneRepository zoneRepository;

  @Override
  @Transactional
  public ZoneResponse addNewZone(AddZoneRequest request) {

    String code = request.getZoneCode().trim().toUpperCase();
    String name = request.getZoneName().trim();

    // Duplicate validations
    if (zoneRepository.existsByCodeIgnoreCase(code)) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST,
        "ZONE_CODE_ALREADY_EXISTS",
        "Zone with code '" + code + "' already exists"
      );
    }

    if (zoneRepository.existsByNameIgnoreCase(name)) {
      throw new BaseException(
        HttpStatus.BAD_REQUEST,
        "ZONE_NAME_ALREADY_EXISTS",
        "Zone with name '" + name + "' already exists"
      );
    }

    ZoneEntity zone = ZoneEntity.builder()
      .code(code)
      .name(name)
      .isActive(true)
      .build();

    ZoneEntity savedZone = zoneRepository.save(zone);

    log.info("Zone created successfully with id: {}", savedZone.getId());

    return mapToResponse(savedZone);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ZoneResponse> getAllZones(String searchTerm) {

    List<ZoneEntity> zones;

    if (searchTerm == null || searchTerm.trim().isEmpty()) {
      zones = zoneRepository.findByIsActiveTrueOrderByNameAsc();
    } else {
      zones = zoneRepository.searchActiveZones(
        searchTerm.trim().toLowerCase()
      );
    }

    return zones.stream()
      .map(this::mapToResponse)
      .toList();
  }

  private ZoneResponse mapToResponse(ZoneEntity zone) {
    return ZoneResponse.builder()
      .id(zone.getId())
      .code(zone.getCode())
      .name(zone.getName())
      .isActive(zone.getIsActive())
      .createdAt(zone.getCreatedAt())
      .build();
  }


}
