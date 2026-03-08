package com.railway.main_service.repository;

import com.railway.main_service.entity.RouteStopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStopEntity, Long> {

  // All stops for a route — ordered by stop_number
  @Query("SELECT rs FROM RouteStopEntity rs " +
    "JOIN FETCH rs.station s " +
    "WHERE rs.route.routeId = :routeId " +
    "ORDER BY rs.stopNumber ASC")
  List<RouteStopEntity> findAllByRouteId(@Param("routeId") Long routeId);

  // Check station already on route
  boolean existsByRoute_RouteIdAndStation_Id(Long routeId, Long stationId);

  // Check stop number already taken on route
  boolean existsByRoute_RouteIdAndStopNumber(Long routeId, Integer stopNumber);

  // Find by stop_id + route_id (ownership check)
  Optional<RouteStopEntity> findByStopIdAndRoute_RouteId(Long stopId, Long routeId);

  // Max stop number on route — used to auto-assign next stop number
  @Query("SELECT COALESCE(MAX(rs.stopNumber), 0) FROM RouteStopEntity rs " +
    "WHERE rs.route.routeId = :routeId")
  int findMaxStopNumber(@Param("routeId") Long routeId);

  // Count stops — for cascade info
  int countByRoute_RouteId(Long routeId);

  // Find last stop (highest stop_number) — used to sync destination + total_km on route
  @Query("SELECT rs FROM RouteStopEntity rs " +
    "WHERE rs.route.routeId = :routeId " +
    "ORDER BY rs.stopNumber DESC LIMIT 1")
  Optional<RouteStopEntity> findLastStop(@Param("routeId") Long routeId);

  // Find first stop — used to sync source on route
  @Query("SELECT rs FROM RouteStopEntity rs " +
    "WHERE rs.route.routeId = :routeId " +
    "ORDER BY rs.stopNumber ASC LIMIT 1")
  Optional<RouteStopEntity> findFirstStop(@Param("routeId") Long routeId);

  // Shift stop numbers up by 1 from a given position — used for inserting mid-route
  @Modifying
  @Query("UPDATE RouteStopEntity rs SET rs.stopNumber = rs.stopNumber + 1 " +
    "WHERE rs.route.routeId = :routeId AND rs.stopNumber >= :fromStop")
  void shiftStopNumbersUp(@Param("routeId") Long routeId, @Param("fromStop") int fromStop);

  // Shift stop numbers down by 1 from a given position — used for deleting mid-route stop
  @Modifying
  @Query("UPDATE RouteStopEntity rs SET rs.stopNumber = rs.stopNumber - 1 " +
    "WHERE rs.route.routeId = :routeId AND rs.stopNumber > :afterStop")
  void shiftStopNumbersDown(@Param("routeId") Long routeId, @Param("afterStop") int afterStop);
}
