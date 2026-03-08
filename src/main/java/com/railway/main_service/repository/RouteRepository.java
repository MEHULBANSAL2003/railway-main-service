package com.railway.main_service.repository;

import com.railway.main_service.entity.RouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<RouteEntity, Long> {

  boolean existsByRouteCode(String routeCode);
  boolean existsByRouteName(String routeName);
  boolean existsByRouteNameAndRouteCodeNot(String routeName, String routeCode);

  Optional<RouteEntity> findByRouteCode(String routeCode);

  // Admin list — search by code or name, with source/dest eagerly loaded
  @Query("SELECT r FROM RouteEntity r " +
    "LEFT JOIN FETCH r.sourceStation ss " +
    "LEFT JOIN FETCH r.destinationStation ds " +
    "WHERE (:search IS NULL " +
    "   OR LOWER(r.routeCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
    "   OR LOWER(r.routeName) LIKE LOWER(CONCAT('%', :search, '%')) " +
    "   OR LOWER(ss.stationName) LIKE LOWER(CONCAT('%', :search, '%')) " +
    "   OR LOWER(ds.stationName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
    "ORDER BY r.routeCode ASC")
  List<RouteEntity> findAllForAdmin(@Param("search") String search);

  // Dropdown — active routes only
  @Query("SELECT r FROM RouteEntity r " +
    "LEFT JOIN FETCH r.sourceStation " +
    "LEFT JOIN FETCH r.destinationStation " +
    "WHERE r.isActive = true " +
    "AND (:search IS NULL " +
    "  OR LOWER(r.routeCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
    "  OR LOWER(r.routeName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
    "ORDER BY r.routeCode ASC")
  List<RouteEntity> findActiveForDropdown(@Param("search") String search);

  // Check if any active train uses this route — for cascade info
  @Query("SELECT COUNT(t) FROM TrainEntity t WHERE t.route.routeId = :routeId AND t.isActive = true")
  int countActiveTrainsByRouteId(@Param("routeId") Long routeId);

  // Count all trains on this route
  @Query("SELECT COUNT(t) FROM TrainEntity t WHERE t.route.routeId = :routeId")
  int countTrainsByRouteId(@Param("routeId") Long routeId);
}
