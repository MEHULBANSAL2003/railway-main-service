package com.railway.main_service.repository;

import com.railway.main_service.entity.TrainCoachEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainCoachRepository extends JpaRepository<TrainCoachEntity, Long> {

  // ── Current active row per coach type (effective today) ───────────────────
  // Used for the main coach cards view — shows one card per coach type
  @Query("""
      SELECT tc FROM TrainCoachEntity tc
      JOIN FETCH tc.coachType ct
      WHERE tc.train.trainId  = :trainId
        AND tc.effectiveFrom <= :today
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :today)
      ORDER BY ct.typeCode ASC
      """)
  List<TrainCoachEntity> findCurrentByTrainId(
    @Param("trainId") Long trainId,
    @Param("today")   LocalDate today);

  // ── Full history for one coach type — all rows sorted by date ─────────────
  // Used in the history drawer/panel
  @Query("""
      SELECT tc FROM TrainCoachEntity tc
      JOIN FETCH tc.coachType ct
      WHERE tc.train.trainId      = :trainId
        AND tc.coachType.typeCode = :typeCode
      ORDER BY tc.effectiveFrom ASC
      """)
  List<TrainCoachEntity> findHistoryByTrainIdAndTypeCode(
    @Param("trainId")  Long   trainId,
    @Param("typeCode") String typeCode);

  // ── All rows for a train — used internally only (e.g. copy coaches) ───────
  @Query("""
      SELECT tc FROM TrainCoachEntity tc
      JOIN FETCH tc.coachType ct
      WHERE tc.train.trainId = :trainId
      ORDER BY ct.typeCode ASC
      """)
  List<TrainCoachEntity> findAllByTrainId(@Param("trainId") Long trainId);

  // ── Active rows for inventory init (date-aware) ───────────────────────────
  @Query("""
      SELECT tc FROM TrainCoachEntity tc
      JOIN FETCH tc.coachType ct
      WHERE tc.train.trainId  = :trainId
        AND tc.effectiveFrom <= :journeyDate
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :journeyDate)
      """)
  List<TrainCoachEntity> findActiveCoachesForDate(
    @Param("trainId")     Long      trainId,
    @Param("journeyDate") LocalDate journeyDate);

  // ── Legacy isActive-based query (kept for any callers not yet migrated) ───
  List<TrainCoachEntity> findByTrain_TrainIdAndIsActiveTrue(Long trainId);

  // ── Existence checks ──────────────────────────────────────────────────────
  // Check if ANY row exists for train+coachType (used when adding new type)
  boolean existsByTrain_TrainIdAndCoachType_TypeId(Long trainId, Long coachTypeId);

  // Check if an ACTIVE (open-ended) row already exists — prevents duplicate active rows
  @Query("""
      SELECT COUNT(tc) > 0 FROM TrainCoachEntity tc
      WHERE tc.train.trainId      = :trainId
        AND tc.coachType.typeId   = :coachTypeId
        AND tc.effectiveTo IS NULL
      """)
  boolean existsActiveRowByTrainAndCoachType(
    @Param("trainId")    Long trainId,
    @Param("coachTypeId") Long coachTypeId);

  Optional<TrainCoachEntity> findByCoachIdAndTrain_TrainId(Long coachId, Long trainId);

  int countByTrain_TrainIdAndIsActiveTrue(Long trainId);
  int countByTrain_TrainId(Long trainId);
  int countByCoachType_TypeCodeAndIsActiveTrue(String typeCode);

  @Query("SELECT tc.coachType.typeId FROM TrainCoachEntity tc WHERE tc.train.trainId = :trainId")
  List<Long> findUsedCoachTypeIdsByTrainId(@Param("trainId") Long trainId);

  @Query("""
      SELECT tc FROM TrainCoachEntity tc
      WHERE tc.train.trainId    = :trainId
        AND tc.coachType.typeId = :coachTypeId
        AND tc.effectiveFrom   <= :dateTo
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :dateFrom)
      """)
  List<TrainCoachEntity> findOverlapping(
    @Param("trainId")     Long      trainId,
    @Param("coachTypeId") Long      coachTypeId,
    @Param("dateFrom")    LocalDate dateFrom,
    @Param("dateTo")      LocalDate dateTo);

}
