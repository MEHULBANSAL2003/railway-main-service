package com.railway.main_service.repository;

import com.railway.main_service.entity.TrainCoachEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainCoachRepository extends JpaRepository<TrainCoachEntity, Long> {

  // All coaches for a train — used in sub-page load
  @Query("SELECT tc FROM TrainCoachEntity tc " +
    "JOIN FETCH tc.coachType ct " +
    "WHERE tc.train.trainId = :trainId " +
    "ORDER BY ct.typeCode ASC")
  List<TrainCoachEntity> findAllByTrainId(@Param("trainId") Long trainId);

  List<TrainCoachEntity> findByTrain_TrainIdAndIsActiveTrue(@Param("trainId") Long trainId);

  // Check duplicate before add
  boolean existsByTrain_TrainIdAndCoachType_TypeId(Long trainId, Long coachTypeId);

  // Exclude self on update (not needed here since coachType is immutable,
  // but kept for safety)
  Optional<TrainCoachEntity> findByTrain_TrainIdAndCoachType_TypeCode(
    Long trainId, String typeCode);

  // Fetch single by coachId + trainId (ownership check)
  Optional<TrainCoachEntity> findByCoachIdAndTrain_TrainId(Long coachId, Long trainId);

  // Count active coaches for a train — used in cascade info
  int countByTrain_TrainIdAndIsActiveTrue(Long trainId);

  // Count all coaches for cascade display
  int countByTrain_TrainId(Long trainId);

  // Used when deactivating a coachType — cascade count
  int countByCoachType_TypeCodeAndIsActiveTrue(String typeCode);

  @Query("SELECT tc.coachType.typeId FROM TrainCoachEntity tc WHERE tc.train.trainId = :trainId")
   List<Long> findUsedCoachTypeIdsByTrainId(@Param("trainId") Long trainId);


  @Query("""
    SELECT tc FROM TrainCoachEntity tc
    JOIN FETCH tc.coachType ct
    WHERE tc.train.trainId = :trainId
      AND tc.effectiveFrom <= :journeyDate
      AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :journeyDate)
    """)
  List<TrainCoachEntity> findActiveCoachesForDate(
    @Param("trainId")     Long      trainId,
    @Param("journeyDate") LocalDate journeyDate);

  // Used in change-config validation — find all coaches for a train
// that overlap with a given date range (for conflict checking)
  @Query("""
    SELECT tc FROM TrainCoachEntity tc
    WHERE tc.train.trainId  = :trainId
      AND tc.coachType.typeId = :coachTypeId
      AND tc.effectiveFrom  <= :dateTo
      AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :dateFrom)
    """)
  List<TrainCoachEntity> findOverlapping(
    @Param("trainId")     Long      trainId,
    @Param("coachTypeId") Long      coachTypeId,
    @Param("dateFrom") LocalDate dateFrom,
    @Param("dateTo")      LocalDate dateTo);

}
