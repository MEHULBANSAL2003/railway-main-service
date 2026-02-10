package com.railway.main_service.repository;

import com.railway.main_service.entity.StationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StationRepository extends JpaRepository<StationEntity, Long> {

  boolean existsByStationCode(String stationCode);
}
