package com.railway.main_service.repository;

import com.railway.main_service.entity.CityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<CityEntity, Long> {

  Optional<CityEntity> findByNameIgnoreCaseAndStateId(String name, Long stateId);

  boolean existsByNameIgnoreCaseAndStateId(String name, Long stateId);

  // Get all cities by state ID
  List<CityEntity> findAllByStateIdAndIsActiveTrueOrderByName(Long stateId);

  // Get all cities by state code
  @Query("SELECT c FROM CityEntity c JOIN FETCH c.state s WHERE s.code = :stateCode AND c.isActive = true ORDER BY c.name")
  List<CityEntity> findAllByStateCodeAndIsActiveTrue(@Param("stateCode") String stateCode);

  // Search cities globally
  @Query("SELECT c FROM CityEntity c JOIN FETCH c.state WHERE " +
    "c.isActive = true AND " +
    "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
    "ORDER BY c.name")
  List<CityEntity> searchByNameIgnoreCaseContaining(@Param("query") String query);

  // Count active cities
  @Query("SELECT COUNT(c) FROM CityEntity c WHERE c.isActive = true")
  long countActiveCities();

  // Count cities by state
  @Query("SELECT COUNT(c) FROM CityEntity c WHERE c.state.id = :stateId AND c.isActive = true")
  long countActiveCitiesByStateId(@Param("stateId") Long stateId);
}
