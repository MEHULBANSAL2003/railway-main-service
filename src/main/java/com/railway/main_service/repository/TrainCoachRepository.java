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

  // ── Current active rows (effective today) ─────────────────────────────────
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

  // ── Inactive rows — effectiveTo is in the past ────────────────────────────
  @Query("""
    SELECT tc FROM TrainCoachEntity tc
    JOIN FETCH tc.coachType ct
    WHERE tc.train.trainId  = :trainId
      AND tc.effectiveTo IS NOT NULL
      AND tc.effectiveTo     < :today
      AND NOT EXISTS (
        SELECT 1 FROM TrainCoachEntity tc2
        WHERE tc2.train.trainId    = tc.train.trainId
          AND tc2.coachType.typeId = tc.coachType.typeId
          AND tc2.effectiveFrom   <= :today
          AND (tc2.effectiveTo IS NULL OR tc2.effectiveTo >= :today)
      )
    ORDER BY ct.typeCode ASC, tc.effectiveTo DESC
    """)
  List<TrainCoachEntity> findInactiveByTrainId(
    @Param("trainId") Long      trainId,
    @Param("today")   LocalDate today);

  // ── Full history for one coach type ───────────────────────────────────────
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

  // ── All rows for a train (copy coaches) ───────────────────────────────────
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

  List<TrainCoachEntity> findByTrain_TrainIdAndIsActiveTrue(Long trainId);

  boolean existsByTrain_TrainIdAndCoachType_TypeId(Long trainId, Long coachTypeId);

  @Query("""
      SELECT COUNT(tc) > 0 FROM TrainCoachEntity tc
      WHERE tc.train.trainId      = :trainId
        AND tc.coachType.typeId   = :coachTypeId
        AND tc.effectiveTo IS NULL
      """)
  boolean existsActiveRowByTrainAndCoachType(
    @Param("trainId")     Long trainId,
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

  @Query("""
    SELECT tc FROM TrainCoachEntity tc
    JOIN FETCH tc.coachType ct
    WHERE tc.train.trainId = :trainId
    ORDER BY ct.typeCode ASC, tc.effectiveFrom ASC
    """)
  List<TrainCoachEntity> findAllWithHistoryByTrainId(@Param("trainId") Long trainId);
}
