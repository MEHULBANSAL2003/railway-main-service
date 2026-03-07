package com.railway.main_service.repository;

import com.railway.main_service.entity.TrainTypeCoachEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainTypeCoachRepository extends JpaRepository<TrainTypeCoachEntity, Long> {

  // All allowed coach types for a train type
  @Query("SELECT ttc FROM TrainTypeCoachEntity ttc " +
    "JOIN FETCH ttc.coachType ct " +
    "WHERE ttc.trainType.typeId = :trainTypeId " +
    "ORDER BY ct.typeCode ASC")
  List<TrainTypeCoachEntity> findAllByTrainTypeId(@Param("trainTypeId") Long trainTypeId);

  // Allowed coach type IDs for a train type — used in dropdown filter
  @Query("SELECT ttc.coachType.typeId FROM TrainTypeCoachEntity ttc " +
    "WHERE ttc.trainType.typeId = :trainTypeId")
  List<Long> findAllowedCoachTypeIds(@Param("trainTypeId") Long trainTypeId);

  // Delete all allowed coaches for a train type — used in full replace
  @Modifying
  @Query("DELETE FROM TrainTypeCoachEntity ttc WHERE ttc.trainType.typeId = :trainTypeId")
  void deleteAllByTrainTypeId(@Param("trainTypeId") Long trainTypeId);

  // Check if a specific coach type is allowed for a train type
  boolean existsByTrainType_TypeIdAndCoachType_TypeId(Long trainTypeId, Long coachTypeId);

  // Count — for cascade info display
  int countByTrainType_TypeCode(String typeCode);
}
