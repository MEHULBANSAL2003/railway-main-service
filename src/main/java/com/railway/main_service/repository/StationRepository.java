package com.railway.main_service.repository;

import com.railway.main_service.entity.StationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface StationRepository
  extends JpaRepository<StationEntity, Long>,
  JpaSpecificationExecutor<StationEntity> {

  boolean existsByStationCode(String stationCode);
  boolean existsByStationName(String stationName);

  boolean existsByStationNameOrStationCode(String stationName, String stationCode);

  // ── Existence checks (exclude deleted) ──────────────────
  boolean existsByStationCodeAndIsPermanentlyDeletedFalse(String stationCode);

  boolean existsByStationNameAndIsPermanentlyDeletedFalse(String stationName);

  boolean existsByStationNameAndStationCodeNotAndIsPermanentlyDeletedFalse(
    String stationName, String stationCode);

  // ── Used for Excel/bulk upload duplicate code check ──────
  @Query("SELECT s.stationCode FROM StationEntity s WHERE s.isPermanentlyDeleted = false")
  Set<String> findAllStationCodes();

  // ── Normal lookup — excludes deleted ─────────────────────
  @Query("SELECT s FROM StationEntity s " +
    "WHERE s.stationCode = :stationCode " +
    "AND s.isPermanentlyDeleted = false")
  Optional<StationEntity> findByStationCode(@Param("stationCode") String stationCode);

  // ── Used ONLY inside deleteStation — includes deleted ────
  // Needed to detect "already deleted" and return proper error
  @Query("SELECT s FROM StationEntity s WHERE s.stationCode = :stationCode")
  Optional<StationEntity> findByStationCodeIncludeDeleted(@Param("stationCode") String stationCode);

  // ── Paginated list (no filters) — excludes deleted ───────
  @Query(
    value = "SELECT s FROM StationEntity s " +
      "JOIN FETCH s.city c " +
      "JOIN FETCH c.state " +
      "JOIN FETCH s.zone " +
      "WHERE s.isPermanentlyDeleted = false",
    countQuery = "SELECT COUNT(s) FROM StationEntity s WHERE s.isPermanentlyDeleted = false"
  )
  Page<StationEntity> findAllWithDetails(Pageable pageable);

  @Query(
    value = "SELECT s FROM StationEntity s " +
      "JOIN FETCH s.city c " +
      "JOIN FETCH c.state " +
      "JOIN FETCH s.zone " +
      "WHERE s.isPermanentlyDeleted = false",
    countQuery = "SELECT COUNT(s) FROM StationEntity s WHERE s.isPermanentlyDeleted = true"
  )
  Page<StationEntity> findAllPermanentlyDeletedWithDetails(Pageable pageable);
}
