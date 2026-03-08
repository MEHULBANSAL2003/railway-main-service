package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.request.route.*;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.route.RouteResponse;
import com.railway.main_service.dto.response.route.RouteStopResponse;
import com.railway.main_service.service.routeService.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.ROUTES)
@RequiredArgsConstructor
public class RouteController {

  private final RouteService routeService;

  // ── Route endpoints ───────────────────────────────────────────────────────

  // GET  /api/main/routes/admin?search=
  @GetMapping(ApiConstants.ROUTES_ADMIN)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<RouteResponse>>> getAllForAdmin(
    @RequestParam(required = false) String search) {
    return ResponseEntity.ok(ApiResponse.success(routeService.getAllForAdmin(search)));
  }

  // GET  /api/main/routes/dropdown?search=
  @GetMapping(ApiConstants.ROUTES_DROPDOWN)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<RouteResponse>>> getAllForDropdown(
    @RequestParam(required = false) String search) {
    return ResponseEntity.ok(ApiResponse.success(routeService.getAllForDropdown(search)));
  }

  // GET  /api/main/routes/{routeCode}
  @GetMapping(ApiConstants.ROUTE_BY_CODE)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<RouteResponse>> getRouteWithStops(
    @PathVariable String routeCode) {
    return ResponseEntity.ok(ApiResponse.success(routeService.getRouteWithStops(routeCode)));
  }

  // POST /api/main/routes/add
  @PostMapping(ApiConstants.ROUTE_ADD)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<RouteResponse>> addRoute(
    @Valid @RequestBody AddRouteRequest request) {
    return ResponseEntity.ok(ApiResponse.success(routeService.addRoute(request)));
  }

  // PATCH /api/main/routes/{routeCode}
  @PostMapping(ApiConstants.ROUTE_BY_CODE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<RouteResponse>> updateRoute(
    @PathVariable String routeCode,
    @Valid @RequestBody UpdateRouteRequest request) {
    return ResponseEntity.ok(ApiResponse.success(routeService.updateRoute(routeCode, request)));
  }

  // PATCH /api/main/routes/{routeCode}/status
  @PostMapping(ApiConstants.ROUTE_STATUS)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<RouteResponse>> toggleStatus(
    @PathVariable String routeCode,
    @RequestParam boolean isActive) {
    return ResponseEntity.ok(ApiResponse.success(routeService.toggleStatus(routeCode, isActive)));
  }

  // GET  /api/main/routes/{routeCode}/cascade-info
  @GetMapping(ApiConstants.ROUTE_CASCADE_INFO)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CascadeInfoResponse>> getCascadeInfo(
    @PathVariable String routeCode) {
    return ResponseEntity.ok(ApiResponse.success(routeService.getCascadeInfo(routeCode)));
  }

  // ── Route Stop endpoints ──────────────────────────────────────────────────

  // GET  /api/main/routes/{routeCode}/stops
  @GetMapping(ApiConstants.ROUTE_STOPS)
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<RouteStopResponse>>> getStops(
    @PathVariable String routeCode) {
    return ResponseEntity.ok(ApiResponse.success(routeService.getStops(routeCode)));
  }

  // POST /api/main/routes/{routeCode}/stops/add
  @PostMapping(ApiConstants.ROUTE_STOP_ADD)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<RouteStopResponse>> addStop(
    @PathVariable String routeCode,
    @Valid @RequestBody AddRouteStopRequest request) {
    return ResponseEntity.ok(ApiResponse.success(routeService.addStop(routeCode, request)));
  }

  // PATCH /api/main/routes/{routeCode}/stops/{stopId}
  @PostMapping(ApiConstants.ROUTE_STOP_UPDATE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<RouteStopResponse>> updateStop(
    @PathVariable String routeCode,
    @PathVariable Long stopId,
    @Valid @RequestBody UpdateRouteStopRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
      routeService.updateStop(routeCode, stopId, request)));
  }

  // DELETE /api/main/routes/{routeCode}/stops/{stopId}
  @DeleteMapping(ApiConstants.ROUTE_STOP_UPDATE)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> deleteStop(
    @PathVariable String routeCode,
    @PathVariable Long stopId) {
    routeService.deleteStop(routeCode, stopId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
