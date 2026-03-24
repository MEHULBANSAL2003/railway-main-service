package com.railway.main_service.repository;


import com.railway.main_service.entity.StateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StateRepository extends JpaRepository<StateEntity, Long> {

  Optional<StateEntity> findByCode(String code);

  Optional<StateEntity> findByName(String name);

  @Query("SELECT s FROM StateEntity s " +
    "WHERE s.effectiveFrom <= CURRENT_DATE AND (s.effectiveTill IS NULL OR s.effectiveTill > CURRENT_DATE) " +
    "ORDER BY s.name ASC")
  List<StateEntity> findAllActiveOrderByName();

  boolean existsByCode(String code);

  boolean existsByName(String name);

  @Query("""
    SELECT s FROM StateEntity s
    WHERE s.effectiveFrom <= CURRENT_DATE AND (s.effectiveTill IS NULL OR s.effectiveTill > CURRENT_DATE)
    AND (
        LOWER(s.name) LIKE LOWER(CONCAT(:searchTerm, '%'))
        OR LOWER(s.code) LIKE LOWER(CONCAT(:searchTerm, '%'))
    )
""")
  List<StateEntity> searchActiveStates(String searchTerm);

  @Query("SELECT s FROM StateEntity s " +
    "WHERE s.effectiveFrom <= CURRENT_DATE AND (s.effectiveTill IS NULL OR s.effectiveTill > CURRENT_DATE)")
  List<StateEntity> findAllActive();

  Optional<StateEntity> findByNameIgnoreCase(String name);
}
