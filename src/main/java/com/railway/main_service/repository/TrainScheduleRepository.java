package com.railway.main_service.repository;

import com.railway.main_service.entity.TrainScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainScheduleRepository extends JpaRepository<TrainScheduleEntity, Long> {

  // ── Currently RUNNING ─────────────────────────────────────────────────────
  // active AND startDate <= today AND (endDate IS NULL OR endDate >= today)
  @Query("SELECT s FROM TrainScheduleEntity s " +
    "WHERE s.train.trainId = :trainId " +
    "AND s.effectiveFrom <= CURRENT_DATE AND (s.effectiveTill IS NULL OR s.effectiveTill > CURRENT_DATE) " +
    "AND s.startDate <= :today " +
    "AND (s.endDate IS NULL OR s.endDate >= :today)")
  Optional<TrainScheduleEntity> findRunning(
    @Param("trainId") Long trainId,
    @Param("today")   LocalDate today);

  // ── UPCOMING (active, startDate > today) ───────────────────────────
  @Query("SELECT s FROM TrainScheduleEntity s " +
    "WHERE s.train.trainId = :trainId " +
    "AND s.effectiveFrom <= CURRENT_DATE AND (s.effectiveTill IS NULL OR s.effectiveTill > CURRENT_DATE) " +
    "AND s.startDate > :today " +
    "ORDER BY s.startDate ASC")
  List<TrainScheduleEntity> findUpcoming(
    @Param("trainId") Long trainId,
    @Param("today")   LocalDate today);

  // ── PAST (active, endDate < today) ────────────────────────────────
  @Query("SELECT s FROM TrainScheduleEntity s " +
    "WHERE s.train.trainId = :trainId " +
    "AND s.effectiveFrom <= CURRENT_DATE AND (s.effectiveTill IS NULL OR s.effectiveTill > CURRENT_DATE) " +
    "AND s.endDate IS NOT NULL " +
    "AND s.endDate < :today " +
    "ORDER BY s.startDate DESC")
  List<TrainScheduleEntity> findPast(
    @Param("trainId") Long trainId,
    @Param("today")   LocalDate today);

  // ── DEACTIVATED (effectiveTill is set and <= today) ─────────────────────────────────
  @Query("SELECT s FROM TrainScheduleEntity s " +
    "WHERE s.train.trainId = :trainId " +
    "AND s.effectiveTill IS NOT NULL AND s.effectiveTill <= CURRENT_DATE " +
    "ORDER BY s.startDate DESC")
  List<TrainScheduleEntity> findDeactivated(@Param("trainId") Long trainId);

  // ── Ownership check ───────────────────────────────────────────────────────
  Optional<TrainScheduleEntity> findByScheduleIdAndTrain_TrainId(
    Long scheduleId, Long trainId);

  // ── Any active upcoming exists? (for block check on create) ──────────────
  @Query("SELECT COUNT(s) > 0 FROM TrainScheduleEntity s " +
    "WHERE s.train.trainId = :trainId " +
    "AND s.effectiveFrom <= CURRENT_DATE AND (s.effectiveTill IS NULL OR s.effectiveTill > CURRENT_DATE) " +
    "AND s.startDate > :today")
  boolean hasActiveUpcoming(
    @Param("trainId") Long trainId,
    @Param("today")   LocalDate today);

  @Query("SELECT s FROM TrainScheduleEntity s " +
    "JOIN FETCH s.train t " +
    "WHERE s.effectiveFrom <= CURRENT_DATE AND (s.effectiveTill IS NULL OR s.effectiveTill > CURRENT_DATE) " +
    "AND s.startDate <= :targetDate " +
    "AND (s.endDate IS NULL OR s.endDate >= :targetDate)")
  List<TrainScheduleEntity> findActiveSchedulesForDate(
    @Param("targetDate") LocalDate targetDate);
}
