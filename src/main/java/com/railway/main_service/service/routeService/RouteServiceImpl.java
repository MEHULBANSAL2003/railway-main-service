package com.railway.main_service.service.routeService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.route.*;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.route.RouteResponse;
import com.railway.main_service.dto.response.route.RouteStopResponse;
import com.railway.main_service.entity.RouteEntity;
import com.railway.main_service.entity.RouteStopEntity;
import com.railway.main_service.entity.StationEntity;
import com.railway.main_service.repository.RouteRepository;
import com.railway.main_service.repository.RouteStopRepository;
import com.railway.main_service.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @Loggable @Slf4j @RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

  private final RouteRepository     routeRepository;
  private final RouteStopRepository routeStopRepository;
  private final StationRepository   stationRepository;

  // ── Add Route ─────────────────────────────────────────────────────────────
  @Override
  @Transactional
  public RouteResponse addRoute(AddRouteRequest request) {
    String code = request.getRouteCode().trim().toUpperCase();
    String name = request.getRouteName().trim();

    if (routeRepository.existsByRouteCode(code))
      throw new BaseException(HttpStatus.CONFLICT, "ROUTE_CODE_EXISTS",
        "Route with code '" + code + "' already exists.");

    if (routeRepository.existsByRouteName(name))
      throw new BaseException(HttpStatus.CONFLICT, "ROUTE_NAME_EXISTS",
        "Route with name '" + name + "' already exists.");

    RouteEntity entity = RouteEntity.builder()
      .routeCode(code)
      .routeName(name)
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    return toResponse(routeRepository.save(entity), 0, "Route created. Add stops to complete it.");
  }

  // ── Update Route ──────────────────────────────────────────────────────────
  @Override
  @Transactional
  public RouteResponse updateRoute(String routeCode, UpdateRouteRequest request) {
    RouteEntity entity = findByCode(routeCode);

    if (request.getRouteName() != null && !request.getRouteName().isBlank()) {
      String name = request.getRouteName().trim();
      if (routeRepository.existsByRouteNameAndRouteCodeNot(name, routeCode.toUpperCase()))
        throw new BaseException(HttpStatus.CONFLICT, "ROUTE_NAME_EXISTS",
          "Another route with name '" + name + "' already exists.");
      entity.setRouteName(name);
    }

    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    int stopCount = routeStopRepository.countByRoute_RouteId(entity.getRouteId());
    return toResponse(routeRepository.save(entity), stopCount, "Route updated successfully.");
  }

  // ── Toggle Status ─────────────────────────────────────────────────────────
  @Override
  @Transactional
  public RouteResponse toggleStatus(String routeCode, boolean isActive) {
    RouteEntity entity = findByCode(routeCode);

    if (!isActive) {
      int activeTrains = routeRepository.countActiveTrainsByRouteId(entity.getRouteId());
      if (activeTrains > 0)
        throw new BaseException(HttpStatus.CONFLICT, "ROUTE_HAS_ACTIVE_TRAINS",
          "Cannot deactivate route — " + activeTrains + " active train(s) are using it. " +
            "Reassign or deactivate those trains first.");
    }

    if (entity.getIsActive().equals(isActive)) return toResponse(entity, 0, null);

    entity.setIsActive(isActive);
    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    routeRepository.save(entity);

    return toResponse(entity, 0,
      isActive ? "Route activated." : "Route deactivated.");
  }

  // ── Cascade Info ──────────────────────────────────────────────────────────
  @Override
  public CascadeInfoResponse getCascadeInfo(String routeCode) {
    RouteEntity entity = findByCode(routeCode);
    int activeTrains = routeRepository.countActiveTrainsByRouteId(entity.getRouteId());
    int totalTrains  = routeRepository.countTrainsByRouteId(entity.getRouteId());
    int stopCount    = routeStopRepository.countByRoute_RouteId(entity.getRouteId());

    String message = activeTrains > 0
      ? activeTrains + " active train(s) use this route. Deactivation is blocked — reassign trains first."
      : totalTrains > 0
      ? totalTrains + " train(s) reference this route (all inactive). Safe to deactivate."
      : "No trains use this route. Safe to deactivate.";

    return CascadeInfoResponse.builder()
      .entityType("ROUTE")
      .entityCode(entity.getRouteCode())
      .entityName(entity.getRouteName())
      .currentlyActive(entity.getIsActive())
      .message(message)
      .build();
  }

  // ── Get all for admin ─────────────────────────────────────────────────────
  @Override
  public List<RouteResponse> getAllForAdmin(String search) {
    String s = (search != null && !search.isBlank()) ? search.trim() : null;
    return routeRepository.findAllForAdmin(s).stream()
      .map(e -> {
        int stops = routeStopRepository.countByRoute_RouteId(e.getRouteId());
        return toResponse(e, stops, null);
      }).toList();
  }

  // ── Get for dropdown ──────────────────────────────────────────────────────
  @Override
  public List<RouteResponse> getAllForDropdown(String search) {
    String s = (search != null && !search.isBlank()) ? search.trim() : null;
    return routeRepository.findActiveForDropdown(s).stream()
      .map(e -> toResponse(e, 0, null)).toList();
  }

  // ── Route detail with stops ───────────────────────────────────────────────
  @Override
  public RouteResponse getRouteWithStops(String routeCode) {
    RouteEntity entity = findByCode(routeCode);
    List<RouteStopResponse> stops = buildStopResponses(
      routeStopRepository.findAllByRouteId(entity.getRouteId())
    );
    RouteResponse response = toResponse(entity, stops.size(), null);
    response.setStops(stops);
    return response;
  }

  // ── Add Stop ──────────────────────────────────────────────────────────────
  @Override
  @Transactional
  public RouteStopResponse addStop(String routeCode, AddRouteStopRequest request) {
    RouteEntity route = findByCode(routeCode);

    String stationCode = request.getStationCode().trim().toUpperCase();
    StationEntity station = stationRepository.findByStationCode(stationCode)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "STATION_NOT_FOUND",
        "Station not found: " + stationCode));

    if (!station.getIsActive())
      throw new BaseException(HttpStatus.BAD_REQUEST, "STATION_INACTIVE",
        "Station '" + stationCode + "' is inactive.");

    // Check station not already on this route
    if (routeStopRepository.existsByRoute_RouteIdAndStation_Id(
      route.getRouteId(), station.getId()))
      throw new BaseException(HttpStatus.CONFLICT, "STOP_ALREADY_EXISTS",
        "Station '" + stationCode + "' is already on this route.");

    // Determine stop number
    int stopNumber;
    if (request.getStopNumber() != null) {
      // Insert at specific position — shift existing stops down
      stopNumber = request.getStopNumber();
      int maxStop = routeStopRepository.findMaxStopNumber(route.getRouteId());
      if (stopNumber <= maxStop) {
        routeStopRepository.shiftStopNumbersUp(route.getRouteId(), stopNumber);
      }
    } else {
      // Append at end
      stopNumber = routeStopRepository.findMaxStopNumber(route.getRouteId()) + 1;
    }

    // Validate km_from_source = 0 for first stop
    if (stopNumber == 1 && request.getKmFromSource() != 0)
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_KM",
        "First stop (source) must have km_from_source = 0.");

    RouteStopEntity stopEntity = RouteStopEntity.builder()
      .route(route)
      .station(station)
      .stopNumber(stopNumber)
      .kmFromSource(request.getKmFromSource())
      .dayNumber(request.getDayNumber())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    RouteStopEntity saved = routeStopRepository.save(stopEntity);

    // Sync route source/destination/totalKm
    syncRouteSummary(route);

    return toStopResponse(saved, null, null);
  }

  // ── Update Stop ───────────────────────────────────────────────────────────
  @Override
  @Transactional
  public RouteStopResponse updateStop(String routeCode, Long stopId,
                                      UpdateRouteStopRequest request) {
    RouteEntity route = findByCode(routeCode);
    RouteStopEntity stop = routeStopRepository
      .findByStopIdAndRoute_RouteId(stopId, route.getRouteId())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "STOP_NOT_FOUND",
        "Stop not found on this route."));

    if (request.getKmFromSource() != null) {
      if (stop.getStopNumber() == 1 && request.getKmFromSource() != 0)
        throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_KM",
          "First stop (source) must have km_from_source = 0.");
      stop.setKmFromSource(request.getKmFromSource());
    }
    if (request.getDayNumber() != null) stop.setDayNumber(request.getDayNumber());
    stop.setUpdatedBy(SecurityUtils.getCurrentAdminId());

    RouteStopEntity saved = routeStopRepository.save(stop);
    syncRouteSummary(route);

    return toStopResponse(saved, null, "Stop updated successfully.");
  }

  // ── Delete Stop ───────────────────────────────────────────────────────────
  @Override
  @Transactional
  public void deleteStop(String routeCode, Long stopId) {
    RouteEntity route = findByCode(routeCode);
    RouteStopEntity stop = routeStopRepository
      .findByStopIdAndRoute_RouteId(stopId, route.getRouteId())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "STOP_NOT_FOUND",
        "Stop not found on this route."));

    int deletedStopNumber = stop.getStopNumber();
    routeStopRepository.delete(stop);

    // Re-sequence stop numbers after deletion
    routeStopRepository.shiftStopNumbersDown(route.getRouteId(), deletedStopNumber);

    // Sync route summary
    syncRouteSummary(route);
  }

  // ── Get Stops ─────────────────────────────────────────────────────────────
  @Override
  public List<RouteStopResponse> getStops(String routeCode) {
    RouteEntity route = findByCode(routeCode);
    return buildStopResponses(
      routeStopRepository.findAllByRouteId(route.getRouteId())
    );
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private RouteEntity findByCode(String code) {
    return routeRepository.findByRouteCode(code.trim().toUpperCase())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND",
        "Route not found: " + code));
  }

  // Sync source station, destination station, total_km after any stop change
  private void syncRouteSummary(RouteEntity route) {
    routeStopRepository.findFirstStop(route.getRouteId()).ifPresent(first -> {
      route.setSourceStation(first.getStation());
    });
    routeStopRepository.findLastStop(route.getRouteId()).ifPresent(last -> {
      route.setDestinationStation(last.getStation());
      route.setTotalKm(last.getKmFromSource());
    });
    // If no stops left — clear summary
    if (routeStopRepository.countByRoute_RouteId(route.getRouteId()) == 0) {
      route.setSourceStation(null);
      route.setDestinationStation(null);
      route.setTotalKm(null);
    }
    routeRepository.save(route);
  }

  // Build stop responses with kmFromPrevious derived
  private List<RouteStopResponse> buildStopResponses(List<RouteStopEntity> stops) {
    List<RouteStopResponse> result = new java.util.ArrayList<>();
    Integer prevKm = null;
    for (RouteStopEntity stop : stops) {
      Integer kmFromPrev = prevKm != null ? stop.getKmFromSource() - prevKm : null;
      result.add(toStopResponse(stop, kmFromPrev, null));
      prevKm = stop.getKmFromSource();
    }
    return result;
  }

  // ── Mappers ───────────────────────────────────────────────────────────────

  private RouteResponse toResponse(RouteEntity e, int stopCount, String message) {
    int activeTrains = routeRepository.countActiveTrainsByRouteId(e.getRouteId());

    RouteResponse.RouteResponseBuilder b = RouteResponse.builder()
      .routeId(e.getRouteId())
      .routeCode(e.getRouteCode())
      .routeName(e.getRouteName())
      .totalKm(e.getTotalKm())
      .totalStops(stopCount)
      .activeTrains(activeTrains)
      .isActive(e.getIsActive())
      .createdBy(e.getCreatedBy())
      .updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt())
      .updatedAt(e.getUpdatedAt())
      .message(message);

    if (e.getSourceStation() != null) {
      b.sourceStationId(e.getSourceStation().getId())
        .sourceStationCode(e.getSourceStation().getStationCode())
        .sourceStationName(e.getSourceStation().getStationName());
    }
    if (e.getDestinationStation() != null) {
      b.destinationStationId(e.getDestinationStation().getId())
        .destinationStationCode(e.getDestinationStation().getStationCode())
        .destinationStationName(e.getDestinationStation().getStationName());
    }
    return b.build();
  }

  private RouteStopResponse toStopResponse(RouteStopEntity e,
                                           Integer kmFromPrevious, String message) {
    return RouteStopResponse.builder()
      .stopId(e.getStopId())
      .routeId(e.getRoute().getRouteId())
      .routeCode(e.getRoute().getRouteCode())
      .stationId(e.getStation().getId())
      .stationCode(e.getStation().getStationCode())
      .stationName(e.getStation().getStationName())
      .stationType(e.getStation().getStationType() != null
        ? e.getStation().getStationType().name() : null)
      .stopNumber(e.getStopNumber())
      .kmFromSource(e.getKmFromSource())
      .dayNumber(e.getDayNumber())
      .kmFromPrevious(kmFromPrevious)
      .createdBy(e.getCreatedBy())
      .updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt())
      .updatedAt(e.getUpdatedAt())
      .message(message)
      .build();
  }
}
