package com.railway.main_service.repository;

import com.railway.main_service.entity.StationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StationRepository extends JpaRepository<StationEntity, Long> {

  boolean existsByStationCode(String stationCode);

  boolean existsByStationName(String stationName);

  Page<StationEntity> findByCity(String city, Pageable pageable);

  Page<StationEntity> findByZone(String zone, Pageable pageable);

  @Query("SELECT s FROM StationEntity s WHERE " +
    "LOWER(s.stationCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
    "LOWER(s.stationName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
    "LOWER(s.city) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
  List<StationEntity> searchStations(@Param("searchTerm") String searchTerm);
}
