package com.railway.main_service.repository;

import com.railway.main_service.entity.StationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface StationRepository extends JpaRepository<StationEntity, Long> {

  boolean existsByStationCode(String stationCode);
  boolean existsByStationName(String stationName);

  @Query("SELECT s.stationCode FROM StationEntity s")
  Set<String> findAllStationCodes();

  @Query(
    value = "SELECT s FROM StationEntity s " +
      "JOIN FETCH s.city c " +
      "JOIN FETCH c.state " +
      "JOIN FETCH s.zone",
    countQuery = "SELECT COUNT(s) FROM StationEntity s"
  )
  Page<StationEntity> findAllWithDetails(Pageable pageable);

  // Search across code, name, city name, state name, zone name
  @Query("SELECT s FROM StationEntity s " +
    "JOIN FETCH s.city c " +
    "JOIN FETCH c.state st " +
    "JOIN FETCH s.zone z " +
    "WHERE LOWER(s.stationCode) LIKE LOWER(CONCAT(:searchTerm, '%')) OR " +
    "LOWER(s.stationName) LIKE LOWER(CONCAT(:searchTerm, '%')) OR " +
    "LOWER(c.name) LIKE LOWER(CONCAT(:searchTerm, '%')) OR " +
    "LOWER(st.name) LIKE LOWER(CONCAT(:searchTerm, '%')) OR " +
    "LOWER(z.name) LIKE LOWER(CONCAT(:searchTerm, '%'))")
  List<StationEntity> searchStations(@Param("searchTerm") String searchTerm);

  Optional<StationEntity> findByStationCode(String stationCode);
}
