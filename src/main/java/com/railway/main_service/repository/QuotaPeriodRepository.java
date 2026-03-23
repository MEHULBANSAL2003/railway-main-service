package com.railway.main_service.repository;

import com.railway.main_service.entity.QuotaPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuotaPeriodRepository extends JpaRepository<QuotaPeriodEntity, Long> {

  // Is the quota active on a given date? (any period covers that date)
  @Query("""
    SELECT COUNT(p) > 0 FROM QuotaPeriodEntity p
    WHERE p.quota.quotaId = :quotaId
      AND p.effectiveFrom <= :date
      AND (p.effectiveTill IS NULL OR p.effectiveTill >= :date)
    """)
  boolean isActiveOnDate(@Param("quotaId") Long quotaId, @Param("date") LocalDate date);

  // Current active period (covering the given date)
  @Query("""
    SELECT p FROM QuotaPeriodEntity p
    WHERE p.quota.quotaId = :quotaId
      AND p.effectiveFrom <= :date
      AND (p.effectiveTill IS NULL OR p.effectiveTill >= :date)
    ORDER BY p.effectiveFrom DESC
    """)
  Optional<QuotaPeriodEntity> findActivePeriod(@Param("quotaId") Long quotaId, @Param("date") LocalDate date);

  // Current open-ended period (effectiveTill IS NULL)
  @Query("""
    SELECT p FROM QuotaPeriodEntity p
    WHERE p.quota.quotaId = :quotaId
      AND p.effectiveTill IS NULL
    ORDER BY p.effectiveFrom DESC
    """)
  Optional<QuotaPeriodEntity> findOpenPeriod(@Param("quotaId") Long quotaId);

  // All periods for a quota — full history ordered by date
  @Query("""
    SELECT p FROM QuotaPeriodEntity p
    WHERE p.quota.quotaId = :quotaId
    ORDER BY p.effectiveFrom ASC
    """)
  List<QuotaPeriodEntity> findAllByQuotaId(@Param("quotaId") Long quotaId);

  // Future periods (effective_from > given date)
  @Query("""
    SELECT p FROM QuotaPeriodEntity p
    WHERE p.quota.quotaId = :quotaId
      AND p.effectiveFrom > :date
    ORDER BY p.effectiveFrom ASC
    """)
  List<QuotaPeriodEntity> findFuturePeriods(@Param("quotaId") Long quotaId, @Param("date") LocalDate date);

  // Close current open period by setting effectiveTill
  @Modifying
  @Query("""
    UPDATE QuotaPeriodEntity p
    SET p.effectiveTill = :tillDate
    WHERE p.quota.quotaId = :quotaId
      AND p.effectiveTill IS NULL
      AND p.effectiveFrom <= :tillDate
    """)
  int closeOpenPeriod(@Param("quotaId") Long quotaId, @Param("tillDate") LocalDate tillDate);

  // Check for overlapping periods
  @Query("""
    SELECT COUNT(p) > 0 FROM QuotaPeriodEntity p
    WHERE p.quota.quotaId = :quotaId
      AND p.effectiveFrom <= :tillDate
      AND (p.effectiveTill IS NULL OR p.effectiveTill >= :fromDate)
      AND (:excludePeriodId IS NULL OR p.periodId != :excludePeriodId)
    """)
  boolean hasOverlap(
    @Param("quotaId") Long quotaId,
    @Param("fromDate") LocalDate fromDate,
    @Param("tillDate") LocalDate tillDate,
    @Param("excludePeriodId") Long excludePeriodId);

  // Delete future periods (used when deactivating permanently)
  @Modifying
  @Query("""
    DELETE FROM QuotaPeriodEntity p
    WHERE p.quota.quotaId = :quotaId
      AND p.effectiveFrom > :afterDate
    """)
  int deleteFuturePeriods(@Param("quotaId") Long quotaId, @Param("afterDate") LocalDate afterDate);
}
