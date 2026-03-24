package com.railway.main_service.repository;


import com.railway.main_service.entity.ZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneRepository extends JpaRepository<ZoneEntity, Long> {

  boolean existsByCodeIgnoreCase(String code);

  boolean existsByNameIgnoreCase(String name);

  @Query("""
        SELECT z FROM ZoneEntity z
        WHERE z.effectiveFrom <= CURRENT_DATE AND (z.effectiveTill IS NULL OR z.effectiveTill > CURRENT_DATE)
        AND (
            LOWER(z.name) LIKE CONCAT(:searchTerm, '%')
            OR LOWER(z.code) LIKE CONCAT(:searchTerm, '%')
        )
    """)
  List<ZoneEntity> searchActiveZones(String searchTerm);

  @Query("SELECT z FROM ZoneEntity z " +
    "WHERE z.effectiveFrom <= CURRENT_DATE AND (z.effectiveTill IS NULL OR z.effectiveTill > CURRENT_DATE) " +
    "ORDER BY z.name ASC")
  List<ZoneEntity> findAllActiveOrderByNameAsc();

  Optional<ZoneEntity> findByCode(String zoneCode);

}
