package com.railway.main_service.repository;

import com.railway.main_service.entity.TrainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainRepository extends JpaRepository<TrainEntity, Long> {

  // ── Existence checks ──────────────────────────────────
  boolean existsByTrainNumber(String trainNumber);

  boolean existsByTrainName(String trainName);

  // Exclude self when checking name conflict on update
  boolean existsByTrainNameAndTrainIdNot(String trainName, Long trainId);

  // ── Fetch by number ───────────────────────────────────
  Optional<TrainEntity> findByTrainNumber(String trainNumber);

  // ── Admin table — search by number or name, with joins ─
  @Query("SELECT t FROM TrainEntity t " +
    "JOIN FETCH t.trainType tt " +
    "JOIN FETCH t.zone z " +
    "WHERE (:search IS NULL " +
    "  OR LOWER(t.trainNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
    "  OR LOWER(t.trainName)   LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
    "ORDER BY t.trainNumber ASC")
  List<TrainEntity> findAllForAdmin(@Param("search") String search);

  // ── Dropdown — active trains only ─────────────────────
  @Query("SELECT t FROM TrainEntity t " +
    "JOIN FETCH t.trainType tt " +
    "JOIN FETCH t.zone z " +
    "WHERE t.isActive = true " +
    "AND (:search IS NULL " +
    "  OR LOWER(t.trainNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
    "  OR LOWER(t.trainName)   LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
    "ORDER BY t.trainNumber ASC")
  List<TrainEntity> findActiveForDropdown(@Param("search") String search);

  // ── Cascade check — count active trains for a type ────
  int countByTrainType_TypeCodeAndIsActiveTrue(String typeCode);

  // ── Cascade check — count active trains for a zone ────
  int countByZone_CodeAndIsActiveTrue(String zoneCode);
}
