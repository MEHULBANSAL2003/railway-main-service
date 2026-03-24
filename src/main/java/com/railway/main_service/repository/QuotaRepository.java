package com.railway.main_service.repository;

import com.railway.main_service.entity.QuotaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotaRepository extends JpaRepository<QuotaEntity, Long> {
  boolean existsByQuotaCode(String quotaCode);
  boolean existsByQuotaName(String quotaName);
  boolean existsByQuotaNameAndQuotaCodeNot(String quotaName, String quotaCode);
  Optional<QuotaEntity> findByQuotaCode(String quotaCode);

  @Query("SELECT q FROM QuotaEntity q " +
    "WHERE q.effectiveFrom <= CURRENT_DATE AND (q.effectiveTill IS NULL OR q.effectiveTill > CURRENT_DATE) " +
    "ORDER BY q.quotaCode ASC")
  List<QuotaEntity> findAllActiveOrderByQuotaCodeAsc();

  List<QuotaEntity> findAllByOrderByQuotaCodeAsc();
}
