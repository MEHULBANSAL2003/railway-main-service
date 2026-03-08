package com.railway.main_service.service.routeService;

import com.railway.main_service.dto.request.route.*;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.route.RouteResponse;
import com.railway.main_service.dto.response.route.RouteStopResponse;

import java.util.List;

public interface RouteService {

  // ── Route CRUD ────────────────────────────────────────────
  RouteResponse addRoute(AddRouteRequest request);
  RouteResponse updateRoute(String routeCode, UpdateRouteRequest request);
  RouteResponse toggleStatus(String routeCode, boolean isActive);
  CascadeInfoResponse getCascadeInfo(String routeCode);

  // ── Route queries ─────────────────────────────────────────
  List<RouteResponse> getAllForAdmin(String search);
  List<RouteResponse> getAllForDropdown(String search);

  // ── Route detail with stops ───────────────────────────────
  RouteResponse getRouteWithStops(String routeCode);

  // ── Route Stop CRUD ───────────────────────────────────────
  RouteStopResponse addStop(String routeCode, AddRouteStopRequest request);
  RouteStopResponse updateStop(String routeCode, Long stopId, UpdateRouteStopRequest request);
  void deleteStop(String routeCode, Long stopId);
  List<RouteStopResponse> getStops(String routeCode);
}
