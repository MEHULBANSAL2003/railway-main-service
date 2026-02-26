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

  List<StateEntity> findAllByIsActiveTrueOrderByName();

  boolean existsByCode(String code);

  boolean existsByName(String name);

  @Query("""
    SELECT s FROM StateEntity s
    WHERE s.isActive = true
    AND (
        LOWER(s.name) LIKE LOWER(CONCAT(:searchTerm, '%'))
        OR LOWER(s.code) LIKE LOWER(CONCAT(:searchTerm, '%'))
    )
""")
  List<StateEntity> searchActiveStates(String searchTerm);

  List<StateEntity> findByIsActiveTrue();

  Optional<StateEntity> findByNameIgnoreCase(String name);
}
