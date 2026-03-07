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
        WHERE z.isActive = true
        AND (
            LOWER(z.name) LIKE CONCAT(:searchTerm, '%')
            OR LOWER(z.code) LIKE CONCAT(:searchTerm, '%')
        )
    """)
  List<ZoneEntity> searchActiveZones(String searchTerm);

  List<ZoneEntity> findByIsActiveTrueOrderByNameAsc();

  Optional<ZoneEntity> findByCode(String zoneCode);

}
