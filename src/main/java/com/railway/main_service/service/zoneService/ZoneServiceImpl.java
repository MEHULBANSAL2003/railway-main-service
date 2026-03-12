package com.railway.main_service.service.zoneService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.pagination.FilterRequest;
import com.railway.common.pagination.PagedResponse;
import com.railway.common.pagination.PageRequestBuilder;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.zone.CreateZoneRequest;
import com.railway.main_service.dto.request.zone.UpdateZoneRequest;
import com.railway.main_service.dto.request.zone.ZoneStatusRequest;
import com.railway.main_service.dto.response.zone.ZoneResponse;
import com.railway.main_service.entity.ZoneEntity;
import com.railway.main_service.repository.ZoneRepository;
import com.railway.main_service.specification.ZoneSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneServiceImpl implements ZoneService {

  private final ZoneRepository zoneRepository;

  // ─────────────────────────────────────────
  // CREATE
  // ─────────────────────────────────────────
  @Override
  @Transactional
  public ZoneResponse createZone(CreateZoneRequest request) {
    log.info("Creating zone with code: {}", request.getZoneCode());

    String code = request.getZoneCode().trim().toUpperCase();

    if (zoneRepository.existsByZoneCodeIgnoreCase(code)) {
      throw new BaseException(HttpStatus.CONFLICT, "ZONE_ALREADY_EXISTS",
        "Zone with code " + code + " already exists. Please update or reactivate it.");
    }

    validateEffectiveFrom(request.getEffectiveFrom());

    ZoneEntity zone = ZoneEntity.builder()
      .zoneName(request.getZoneName().trim())
      .zoneCode(code)
      .effectiveFrom(request.getEffectiveFrom())
      .effectiveTill(null)
      .reason("Initial creation")
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    ZoneEntity saved = zoneRepository.save(zone);
    log.info("Zone created with id: {}", saved.getZoneId());
    return toResponse(saved);
  }

  // ─────────────────────────────────────────
  // UPDATE (name only)
  // ─────────────────────────────────────────
  @Override
  @Transactional
  public ZoneResponse updateZone(Long zoneId, UpdateZoneRequest request) {
    log.info("Updating zone id: {}", zoneId);

    ZoneEntity current = getActiveZone(zoneId);

    validateEffectiveFrom(request.getEffectiveFrom());
    validateEffectiveTill(request.getEffectiveFrom(), request.getEffectiveTill());
    validateNoOverlap(current.getZoneCode(), request.getEffectiveFrom(),
      request.getEffectiveTill(), current.getZoneId());

    current.setEffectiveTill(request.getEffectiveFrom().minusDays(1));
    zoneRepository.save(current);

    ZoneEntity updated = ZoneEntity.builder()
      .zoneName(request.getZoneName().trim())
      .zoneCode(current.getZoneCode())
      .effectiveFrom(request.getEffectiveFrom())
      .effectiveTill(request.getEffectiveTill())
      .reason(request.getReason().trim())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    ZoneEntity saved = zoneRepository.save(updated);
    log.info("Zone updated, new row id: {}", saved.getZoneId());
    return toResponse(saved);
  }

  // ─────────────────────────────────────────
  // DEACTIVATE
  // ─────────────────────────────────────────
  @Override
  @Transactional
  public ZoneResponse deactivateZone(Long zoneId, ZoneStatusRequest request) {
    log.info("Deactivating zone id: {}", zoneId);

    if (request.getEffectiveTill() == null) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
        "Effective till is required for deactivation");
    }

    ZoneEntity current = getActiveZone(zoneId);

    validateEffectiveFrom(request.getEffectiveFrom());
    validateEffectiveTill(request.getEffectiveFrom(), request.getEffectiveTill());
    validateNoOverlap(current.getZoneCode(), request.getEffectiveFrom(),
      request.getEffectiveTill(), current.getZoneId());

    current.setEffectiveTill(request.getEffectiveFrom().minusDays(1));
    zoneRepository.save(current);

    ZoneEntity deactivated = ZoneEntity.builder()
      .zoneName(current.getZoneName())
      .zoneCode(current.getZoneCode())
      .effectiveFrom(request.getEffectiveFrom())
      .effectiveTill(request.getEffectiveTill())
      .reason(request.getReason().trim())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    ZoneEntity saved = zoneRepository.save(deactivated);
    log.info("Zone deactivated from: {} till: {}",
      request.getEffectiveFrom(), request.getEffectiveTill());
    return toResponse(saved);
  }

  // ─────────────────────────────────────────
  // REACTIVATE
  // ─────────────────────────────────────────
  @Override
  @Transactional
  public ZoneResponse reactivateZone(Long zoneId, ZoneStatusRequest request) {
    log.info("Reactivating zone id: {}", zoneId);

    ZoneEntity latest = zoneRepository.findLatestByZoneId(zoneId)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND,
        "ZONE_NOT_FOUND", "Zone not found with id: " + zoneId));

    if (latest.getEffectiveTill() == null) {
      throw new BaseException(HttpStatus.BAD_REQUEST,
        "ZONE_ALREADY_ACTIVE", "Zone is already active");
    }

    if (!request.getEffectiveFrom().isAfter(latest.getEffectiveTill())) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_DATE",
        "Reactivation effective from must be after deactivation date: "
          + latest.getEffectiveTill());
    }

    validateEffectiveTill(request.getEffectiveFrom(), request.getEffectiveTill());
    validateNoOverlap(latest.getZoneCode(), request.getEffectiveFrom(),
      request.getEffectiveTill(), latest.getZoneId());

    ZoneEntity reactivated = ZoneEntity.builder()
      .zoneName(latest.getZoneName())
      .zoneCode(latest.getZoneCode())
      .effectiveFrom(request.getEffectiveFrom())
      .effectiveTill(request.getEffectiveTill())
      .reason(request.getReason().trim())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    ZoneEntity saved = zoneRepository.save(reactivated);
    log.info("Zone reactivated, new row id: {}", saved.getZoneId());
    return toResponse(saved);
  }

  // ─────────────────────────────────────────
  // GET ACTIVE BY ID
  // ─────────────────────────────────────────
  @Override
  public ZoneResponse getActiveZoneById(Long zoneId) {
    return toResponse(getActiveZone(zoneId));
  }

  // ─────────────────────────────────────────
  // GET ALL PAGED
  // ─────────────────────────────────────────
  @Override
  public PagedResponse<ZoneResponse> getAllZones(FilterRequest request) {
    Pageable pageable = PageRequestBuilder.from(request);
    Specification<ZoneEntity> spec = ZoneSpecification.withFilters(request);
    return PagedResponse.of(zoneRepository.findAll(spec, pageable).map(this::toResponse));
  }

  // ─────────────────────────────────────────
  // PRIVATE HELPERS
  // ─────────────────────────────────────────

  private ZoneEntity getActiveZone(Long zoneId) {
    return zoneRepository.findActiveByZoneId(zoneId, LocalDate.now())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND,
        "ZONE_NOT_FOUND", "No active zone found with id: " + zoneId));
  }

  private void validateEffectiveFrom(LocalDate effectiveFrom) {
    if (effectiveFrom.isBefore(LocalDate.now())) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_DATE",
        "Effective from cannot be in the past");
    }
  }

  private void validateEffectiveTill(LocalDate effectiveFrom, LocalDate effectiveTill) {
    if (effectiveTill != null && !effectiveTill.isAfter(effectiveFrom)) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_DATE",
        "Effective till must be after effective from");
    }
  }

  private void validateNoOverlap(String zoneCode, LocalDate from,
                                 LocalDate till, Long excludeId) {
    if (zoneRepository.hasOverlap(zoneCode, from, till, excludeId)) {
      throw new BaseException(HttpStatus.CONFLICT, "ZONE_OVERLAP",
        "An active zone with code " + zoneCode
          + " already exists in this date range");
    }
  }

  private ZoneResponse toResponse(ZoneEntity entity) {
    LocalDate today = LocalDate.now();
    boolean isCurrentlyActive = !entity.getEffectiveFrom().isAfter(today)
      && (entity.getEffectiveTill() == null
      || entity.getEffectiveTill().isAfter(today));

    return ZoneResponse.builder()
      .zoneId(entity.getZoneId())
      .zoneName(entity.getZoneName())
      .zoneCode(entity.getZoneCode())
      .reason(entity.getReason())
      .createdBy(entity.getCreatedBy())
      .createdAt(entity.getCreatedAt())
      .isCurrentlyActive(isCurrentlyActive)
      .build();
  }

  @Override
  @Transactional
  public void createZoneFromExcel(CreateZoneRequest request) {
    createZone(request);
  }
}
