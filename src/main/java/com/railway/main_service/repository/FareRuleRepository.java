package com.railway.main_service.repository;

import com.railway.main_service.entity.FareRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FareRuleRepository extends JpaRepository<FareRuleEntity, Long> {

  // Check duplicate before insert
  boolean existsByTrainType_TypeCodeAndCoachType_TypeCodeAndEffectiveFrom(
    String trainTypeCode, String coachTypeCode, LocalDate effectiveFrom);

  // All rules for admin table — optionally filtered by train type or coach type
  @Query("SELECT f FROM FareRuleEntity f " +
    "JOIN FETCH f.trainType tt JOIN FETCH f.coachType ct " +
    "WHERE (:trainTypeCode IS NULL OR tt.typeCode = CAST(:trainTypeCode AS string)) " +
    "AND (:coachTypeCode IS NULL OR ct.typeCode = CAST(:coachTypeCode AS string)) " +
    "ORDER BY tt.typeCode ASC, ct.typeCode ASC, f.effectiveFrom DESC")
  List<FareRuleEntity> findAllForAdmin(
    @Param("trainTypeCode") String trainTypeCode,
    @Param("coachTypeCode") String coachTypeCode);

  // Current active rule for a specific combo (used by booking-service)
  @Query("SELECT f FROM FareRuleEntity f " +
    "JOIN FETCH f.trainType tt JOIN FETCH f.coachType ct " +
    "WHERE tt.typeCode = :trainTypeCode AND ct.typeCode = :coachTypeCode " +
    "AND f.isActive = true " +
    "AND f.effectiveFrom <= :date " +
    "AND (f.effectiveUntil IS NULL OR f.effectiveUntil >= :date) " +
    "ORDER BY f.effectiveFrom DESC")
  Optional<FareRuleEntity> findCurrentRule(
    @Param("trainTypeCode") String trainTypeCode,
    @Param("coachTypeCode") String coachTypeCode,
    @Param("date") LocalDate date);

  // All history for one combo
  @Query("SELECT f FROM FareRuleEntity f " +
    "JOIN FETCH f.trainType tt JOIN FETCH f.coachType ct " +
    "WHERE tt.typeCode = :trainTypeCode AND ct.typeCode = :coachTypeCode " +
    "ORDER BY f.effectiveFrom DESC")
  List<FareRuleEntity> findAllByCombo(
    @Param("trainTypeCode") String trainTypeCode,
    @Param("coachTypeCode") String coachTypeCode);

  // Most recent active rule for a combo (to close it when new one is added)
  @Query("SELECT f FROM FareRuleEntity f " +
    "JOIN f.trainType tt JOIN f.coachType ct " +
    "WHERE tt.typeCode = :trainTypeCode AND ct.typeCode = :coachTypeCode " +
    "AND f.isActive = true AND f.effectiveUntil IS NULL " +
    "ORDER BY f.effectiveFrom DESC")
  Optional<FareRuleEntity> findOpenRule(
    @Param("trainTypeCode") String trainTypeCode,
    @Param("coachTypeCode") String coachTypeCode);
}
