package com.railway.main_service.repository;

import com.railway.main_service.entity.QuotaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuotaRepository extends JpaRepository<QuotaEntity, Long> {
  boolean existsByQuotaCode(String quotaCode);
  boolean existsByQuotaName(String quotaName);
  boolean existsByQuotaNameAndQuotaCodeNot(String quotaName, String quotaCode);
  Optional<QuotaEntity> findByQuotaCode(String quotaCode);

  // Active quotas on a given date — period-based (replaces findAllByIsActiveTrueOrderByQuotaCodeAsc)
  @Query("""
    SELECT q FROM QuotaEntity q
    WHERE EXISTS (
      SELECT 1 FROM QuotaPeriodEntity p
      WHERE p.quota.quotaId = q.quotaId
        AND p.effectiveFrom <= :today
        AND (p.effectiveTill IS NULL OR p.effectiveTill >= :today)
    )
    ORDER BY q.quotaCode ASC
    """)
  List<QuotaEntity> findActiveOnDate(@Param("today") LocalDate today);

  List<QuotaEntity> findAllByOrderByQuotaCodeAsc();
}
