package com.railway.main_service.repository;

import com.railway.main_service.entity.JourneyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JourneyRepository extends JpaRepository<JourneyEntity, Long> {

  boolean existsByTrain_TrainIdAndJourneyDate(Long trainId, LocalDate journeyDate);

  Optional<JourneyEntity> findByTrain_TrainIdAndJourneyDate(Long trainId, LocalDate journeyDate);

  // All journeys for a train in date range — admin list view
  @Query("SELECT j FROM JourneyEntity j " +
    "JOIN FETCH j.train t " +
    "JOIN FETCH j.schedule s " +
    "WHERE j.train.trainId = :trainId " +
    "AND j.journeyDate BETWEEN :from AND :to " +
    "ORDER BY j.journeyDate ASC")
  List<JourneyEntity> findByTrainAndDateRange(
    @Param("trainId") Long trainId,
    @Param("from")    LocalDate from,
    @Param("to")      LocalDate to);

  // Today's journeys that need chart preparation check
  @Query("SELECT j FROM JourneyEntity j " +
    "JOIN FETCH j.train t " +
    "WHERE j.journeyDate = :today " +
    "AND j.isCancelled = false " +
    "AND j.chartPrepared = false")
  List<JourneyEntity> findTodayJourneysForChartCheck(@Param("today") LocalDate today);

  // With train eagerly loaded — used by detail view
  @Query("SELECT j FROM JourneyEntity j " +
    "JOIN FETCH j.train t " +
    "JOIN FETCH t.trainType " +
    "JOIN FETCH j.schedule s " +
    "WHERE j.journeyId = :journeyId")
  Optional<JourneyEntity> findByIdWithDetails(@Param("journeyId") Long journeyId);

  @Modifying
  @Query("UPDATE JourneyEntity j " +
    "SET j.isCancelled = true, j.cancelReason = :reason, j.updatedAt = CURRENT_TIMESTAMP " +
    "WHERE j.journeyId = :journeyId")
  void cancelJourney(@Param("journeyId") Long journeyId, @Param("reason") String reason);

  @Modifying
  @Query("UPDATE JourneyEntity j " +
    "SET j.chartPrepared = true, j.updatedAt = CURRENT_TIMESTAMP " +
    "WHERE j.journeyId = :journeyId")
  void markChartPrepared(@Param("journeyId") Long journeyId);
}
