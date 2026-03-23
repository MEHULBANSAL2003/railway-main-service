package com.railway.main_service.repository;

import com.railway.main_service.entity.TrainPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainPeriodRepository extends JpaRepository<TrainPeriodEntity, Long> {

  // Is the train active on a given date? (any period covers that date)
  @Query("""
    SELECT COUNT(p) > 0 FROM TrainPeriodEntity p
    WHERE p.train.trainId = :trainId
      AND p.effectiveFrom <= :date
      AND (p.effectiveTill IS NULL OR p.effectiveTill >= :date)
    """)
  boolean isActiveOnDate(@Param("trainId") Long trainId, @Param("date") LocalDate date);

  // Current active period (covering today)
  @Query("""
    SELECT p FROM TrainPeriodEntity p
    WHERE p.train.trainId = :trainId
      AND p.effectiveFrom <= :date
      AND (p.effectiveTill IS NULL OR p.effectiveTill >= :date)
    ORDER BY p.effectiveFrom DESC
    """)
  Optional<TrainPeriodEntity> findActivePeriod(@Param("trainId") Long trainId, @Param("date") LocalDate date);

  // Current open-ended period (effectiveTill IS NULL)
  @Query("""
    SELECT p FROM TrainPeriodEntity p
    WHERE p.train.trainId = :trainId
      AND p.effectiveTill IS NULL
    ORDER BY p.effectiveFrom DESC
    """)
  Optional<TrainPeriodEntity> findOpenPeriod(@Param("trainId") Long trainId);

  // All periods for a train — full history ordered by date
  @Query("""
    SELECT p FROM TrainPeriodEntity p
    WHERE p.train.trainId = :trainId
    ORDER BY p.effectiveFrom ASC
    """)
  List<TrainPeriodEntity> findAllByTrainId(@Param("trainId") Long trainId);

  // Future periods (effective_from > today)
  @Query("""
    SELECT p FROM TrainPeriodEntity p
    WHERE p.train.trainId = :trainId
      AND p.effectiveFrom > :date
    ORDER BY p.effectiveFrom ASC
    """)
  List<TrainPeriodEntity> findFuturePeriods(@Param("trainId") Long trainId, @Param("date") LocalDate date);

  // Close current open period by setting effectiveTill
  @Modifying
  @Query("""
    UPDATE TrainPeriodEntity p
    SET p.effectiveTill = :tillDate
    WHERE p.train.trainId = :trainId
      AND p.effectiveTill IS NULL
      AND p.effectiveFrom <= :tillDate
    """)
  int closeOpenPeriod(@Param("trainId") Long trainId, @Param("tillDate") LocalDate tillDate);

  // Check for overlapping periods
  @Query("""
    SELECT COUNT(p) > 0 FROM TrainPeriodEntity p
    WHERE p.train.trainId = :trainId
      AND p.effectiveFrom <= :tillDate
      AND (p.effectiveTill IS NULL OR p.effectiveTill >= :fromDate)
      AND (:excludePeriodId IS NULL OR p.periodId != :excludePeriodId)
    """)
  boolean hasOverlap(
    @Param("trainId") Long trainId,
    @Param("fromDate") LocalDate fromDate,
    @Param("tillDate") LocalDate tillDate,
    @Param("excludePeriodId") Long excludePeriodId);

  // Delete future periods (used when deactivating permanently)
  @Modifying
  @Query("""
    DELETE FROM TrainPeriodEntity p
    WHERE p.train.trainId = :trainId
      AND p.effectiveFrom > :afterDate
    """)
  int deleteFuturePeriods(@Param("trainId") Long trainId, @Param("afterDate") LocalDate afterDate);
}
