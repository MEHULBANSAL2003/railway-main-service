package com.railway.main_service.repository;

import com.railway.main_service.entity.TrainEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainRepository extends JpaRepository<TrainEntity, Long> {

  // ── Existence checks ──────────────────────────────────────────────────────
  boolean existsByTrainNumber(String trainNumber);
  boolean existsByTrainName(String trainName);
  boolean existsByTrainNameAndTrainIdNot(String trainName, Long trainId);

  // ── Single fetch ──────────────────────────────────────────────────────────
  Optional<TrainEntity> findByTrainNumber(String trainNumber);

  // ── Paginated admin table with filters + sorting ──────────────────────────
  // isActive filter is now derived from train_periods table.
  // isActive param: null = all, true = active on today, false = inactive on today
  @Query(
    value = "SELECT t FROM TrainEntity t " +
      "JOIN t.trainType tt " +
      "JOIN t.zone z " +
      "WHERE (:trainTypeCode IS NULL OR tt.typeCode = CAST(:trainTypeCode AS string)) " +
      "AND   (:zoneCode      IS NULL OR z.code      = CAST(:zoneCode      AS string)) " +
      "AND   (:isActive IS NULL " +
      "  OR (:isActive = true AND EXISTS (" +
      "      SELECT 1 FROM TrainPeriodEntity p " +
      "      WHERE p.train.trainId = t.trainId " +
      "        AND p.effectiveFrom <= :today " +
      "        AND (p.effectiveTill IS NULL OR p.effectiveTill >= :today))) " +
      "  OR (:isActive = false AND NOT EXISTS (" +
      "      SELECT 1 FROM TrainPeriodEntity p " +
      "      WHERE p.train.trainId = t.trainId " +
      "        AND p.effectiveFrom <= :today " +
      "        AND (p.effectiveTill IS NULL OR p.effectiveTill >= :today)))) " +
      "AND   (:search        IS NULL " +
      "    OR LOWER(t.trainNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
      "    OR LOWER(t.trainName)   LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))",
    countQuery =
      "SELECT COUNT(t) FROM TrainEntity t " +
        "JOIN t.trainType tt " +
        "JOIN t.zone z " +
        "WHERE (:trainTypeCode IS NULL OR tt.typeCode = CAST(:trainTypeCode AS string)) " +
        "AND   (:zoneCode      IS NULL OR z.code      = CAST(:zoneCode      AS string)) " +
        "AND   (:isActive IS NULL " +
        "  OR (:isActive = true AND EXISTS (" +
        "      SELECT 1 FROM TrainPeriodEntity p " +
        "      WHERE p.train.trainId = t.trainId " +
        "        AND p.effectiveFrom <= :today " +
        "        AND (p.effectiveTill IS NULL OR p.effectiveTill >= :today))) " +
        "  OR (:isActive = false AND NOT EXISTS (" +
        "      SELECT 1 FROM TrainPeriodEntity p " +
        "      WHERE p.train.trainId = t.trainId " +
        "        AND p.effectiveFrom <= :today " +
        "        AND (p.effectiveTill IS NULL OR p.effectiveTill >= :today)))) " +
        "AND   (:search        IS NULL " +
        "    OR LOWER(t.trainNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
        "    OR LOWER(t.trainName)   LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))"
  )
  Page<TrainEntity> findAllForAdminPaged(
    @Param("trainTypeCode") String trainTypeCode,
    @Param("zoneCode")      String zoneCode,
    @Param("isActive")      Boolean isActive,
    @Param("search")        String search,
    @Param("today")         LocalDate today,
    Pageable pageable
  );

  // ── Dropdown — only trains that are active on today, no pagination ────────
  @Query("SELECT t FROM TrainEntity t " +
    "JOIN FETCH t.trainType tt " +
    "JOIN FETCH t.zone z " +
    "WHERE EXISTS (" +
    "  SELECT 1 FROM TrainPeriodEntity p " +
    "  WHERE p.train.trainId = t.trainId " +
    "    AND p.effectiveFrom <= :today " +
    "    AND (p.effectiveTill IS NULL OR p.effectiveTill >= :today)) " +
    "AND (:search IS NULL " +
    "  OR LOWER(t.trainNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
    "  OR LOWER(t.trainName)   LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
    "ORDER BY t.trainNumber ASC")
  List<TrainEntity> findActiveForDropdown(
    @Param("search") String search,
    @Param("today")  LocalDate today);

  // ── Cascade checks — count trains with an active period on a given date ──
  @Query("SELECT COUNT(t) FROM TrainEntity t " +
    "JOIN t.trainType tt " +
    "WHERE tt.typeCode = :typeCode " +
    "AND EXISTS (" +
    "  SELECT 1 FROM TrainPeriodEntity p " +
    "  WHERE p.train.trainId = t.trainId " +
    "    AND p.effectiveFrom <= :date " +
    "    AND (p.effectiveTill IS NULL OR p.effectiveTill >= :date))")
  int countActiveTrainsByTypeCode(
    @Param("typeCode") String typeCode,
    @Param("date")     LocalDate date);

  @Query("SELECT COUNT(t) FROM TrainEntity t " +
    "JOIN t.zone z " +
    "WHERE z.code = :zoneCode " +
    "AND EXISTS (" +
    "  SELECT 1 FROM TrainPeriodEntity p " +
    "  WHERE p.train.trainId = t.trainId " +
    "    AND p.effectiveFrom <= :date " +
    "    AND (p.effectiveTill IS NULL OR p.effectiveTill >= :date))")
  int countActiveTrainsByZoneCode(
    @Param("zoneCode") String zoneCode,
    @Param("date")     LocalDate date);

  // ── Find trains with active type on date — for cascade blocking check ────
  @Query("SELECT t FROM TrainEntity t " +
    "JOIN t.trainType tt " +
    "WHERE tt.typeCode = :typeCode " +
    "AND EXISTS (" +
    "  SELECT 1 FROM TrainPeriodEntity p " +
    "  WHERE p.train.trainId = t.trainId " +
    "    AND p.effectiveFrom <= :date " +
    "    AND (p.effectiveTill IS NULL OR p.effectiveTill >= :date))")
  List<TrainEntity> findTrainsWithActiveTypeOnDate(
    @Param("typeCode") String typeCode,
    @Param("date")     LocalDate date);

  @Query("SELECT t FROM TrainEntity t " +
    "JOIN FETCH t.trainType " +
    "JOIN FETCH t.zone " +
    "WHERE t.trainNumber = :trainNumber")
  Optional<TrainEntity> findByTrainNumberWithDetails(@Param("trainNumber") String trainNumber);
}
