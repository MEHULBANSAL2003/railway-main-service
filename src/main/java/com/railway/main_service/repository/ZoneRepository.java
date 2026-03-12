package com.railway.main_service.repository;

import com.railway.main_service.entity.ZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ZoneRepository extends JpaRepository<ZoneEntity, Long>,
  JpaSpecificationExecutor<ZoneEntity> {

  boolean existsByZoneCodeIgnoreCase(String zoneCode);

  @Query("""
            SELECT z FROM ZoneEntity z
            WHERE z.zoneId = :zoneId
            AND z.effectiveFrom <= :today
            AND (z.effectiveTill IS NULL OR z.effectiveTill > :today)
            """)
  Optional<ZoneEntity> findActiveByZoneId(
    @Param("zoneId") Long zoneId,
    @Param("today") LocalDate today);

  @Query("""
            SELECT z FROM ZoneEntity z
            WHERE z.zoneId = :zoneId
            ORDER BY z.createdAt DESC
            LIMIT 1
            """)
  Optional<ZoneEntity> findLatestByZoneId(@Param("zoneId") Long zoneId);

  @Query("""
            SELECT COUNT(z) > 0 FROM ZoneEntity z
            WHERE UPPER(z.zoneCode) = UPPER(:zoneCode)
            AND z.zoneId != :excludeId
            AND z.effectiveFrom <= COALESCE(:till, CAST('9999-12-31' AS date))
            AND (z.effectiveTill IS NULL OR z.effectiveTill >= :from)
            """)
  boolean hasOverlap(
    @Param("zoneCode") String zoneCode,
    @Param("from") LocalDate from,
    @Param("till") LocalDate till,
    @Param("excludeId") Long excludeId);
}
