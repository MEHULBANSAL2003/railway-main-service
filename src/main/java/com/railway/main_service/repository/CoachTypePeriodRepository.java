package com.railway.main_service.repository;

import com.railway.main_service.entity.CoachTypePeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CoachTypePeriodRepository extends JpaRepository<CoachTypePeriodEntity, Long> {

  // Is the coach type active on a given date? (any period covers that date)
  @Query("""
    SELECT COUNT(p) > 0 FROM CoachTypePeriodEntity p
    WHERE p.coachType.typeId = :typeId
      AND p.effectiveFrom <= :date
      AND (p.effectiveTill IS NULL OR p.effectiveTill >= :date)
    """)
  boolean isActiveOnDate(@Param("typeId") Long typeId, @Param("date") LocalDate date);

  // Current active period (covering the given date)
  @Query("""
    SELECT p FROM CoachTypePeriodEntity p
    WHERE p.coachType.typeId = :typeId
      AND p.effectiveFrom <= :date
      AND (p.effectiveTill IS NULL OR p.effectiveTill >= :date)
    ORDER BY p.effectiveFrom DESC
    """)
  Optional<CoachTypePeriodEntity> findActivePeriod(@Param("typeId") Long typeId, @Param("date") LocalDate date);

  // Current open-ended period (effectiveTill IS NULL)
  @Query("""
    SELECT p FROM CoachTypePeriodEntity p
    WHERE p.coachType.typeId = :typeId
      AND p.effectiveTill IS NULL
    ORDER BY p.effectiveFrom DESC
    """)
  Optional<CoachTypePeriodEntity> findOpenPeriod(@Param("typeId") Long typeId);

  // All periods for a coach type — full history ordered by date
  @Query("""
    SELECT p FROM CoachTypePeriodEntity p
    WHERE p.coachType.typeId = :typeId
    ORDER BY p.effectiveFrom ASC
    """)
  List<CoachTypePeriodEntity> findAllByTypeId(@Param("typeId") Long typeId);

  // Future periods (effective_from > given date)
  @Query("""
    SELECT p FROM CoachTypePeriodEntity p
    WHERE p.coachType.typeId = :typeId
      AND p.effectiveFrom > :date
    ORDER BY p.effectiveFrom ASC
    """)
  List<CoachTypePeriodEntity> findFuturePeriods(@Param("typeId") Long typeId, @Param("date") LocalDate date);

  // Close current open period by setting effectiveTill
  @Modifying
  @Query("""
    UPDATE CoachTypePeriodEntity p
    SET p.effectiveTill = :tillDate
    WHERE p.coachType.typeId = :typeId
      AND p.effectiveTill IS NULL
      AND p.effectiveFrom <= :tillDate
    """)
  int closeOpenPeriod(@Param("typeId") Long typeId, @Param("tillDate") LocalDate tillDate);

  // Check for overlapping periods
  @Query("""
    SELECT COUNT(p) > 0 FROM CoachTypePeriodEntity p
    WHERE p.coachType.typeId = :typeId
      AND p.effectiveFrom <= :tillDate
      AND (p.effectiveTill IS NULL OR p.effectiveTill >= :fromDate)
      AND (:excludePeriodId IS NULL OR p.periodId != :excludePeriodId)
    """)
  boolean hasOverlap(
    @Param("typeId") Long typeId,
    @Param("fromDate") LocalDate fromDate,
    @Param("tillDate") LocalDate tillDate,
    @Param("excludePeriodId") Long excludePeriodId);

  // Delete future periods (used when deactivating permanently)
  @Modifying
  @Query("""
    DELETE FROM CoachTypePeriodEntity p
    WHERE p.coachType.typeId = :typeId
      AND p.effectiveFrom > :afterDate
    """)
  int deleteFuturePeriods(@Param("typeId") Long typeId, @Param("afterDate") LocalDate afterDate);
}
