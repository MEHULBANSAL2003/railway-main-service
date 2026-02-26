package com.railway.main_service.repository;

import com.railway.main_service.entity.CityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<CityEntity, Long> {

  Page<CityEntity> findAll(Pageable pageable);

  boolean existsByNameIgnoreCaseAndStateId(String name, Long stateId);

  // Starts with search - no null issue since we branch in service
  @Query("SELECT c FROM CityEntity c WHERE LOWER(c.name) LIKE LOWER(CONCAT(:searchTerm, '%'))")
  Page<CityEntity> findAllByNameStartingWith(@Param("searchTerm") String searchTerm, Pageable pageable);

  @Query("SELECT c FROM CityEntity c JOIN c.state s WHERE LOWER(s.name) = LOWER(:stateName)")
  Page<CityEntity> findByStateName(@Param("stateName") String stateName, Pageable pageable);

  @Query("SELECT c FROM CityEntity c JOIN c.state s WHERE " +
    "LOWER(s.name) = LOWER(:stateName) AND " +
    "LOWER(c.name) LIKE LOWER(CONCAT(:searchTerm, '%'))")
  Page<CityEntity> findByStateNameAndNameStartingWith(
    @Param("stateName") String stateName,
    @Param("searchTerm") String searchTerm,
    Pageable pageable);
}
