package com.railway.main_service.repository;

import com.railway.main_service.entity.JourneySeatInventoryEntity;
import com.railway.main_service.enums.QuotaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JourneySeatInventoryRepository extends JpaRepository<JourneySeatInventoryEntity, Long> {

  // All inventory rows for a journey (used for availability tab display)
  @Query("""
      SELECT i FROM JourneySeatInventoryEntity i
      JOIN FETCH i.trainCoach tc
      JOIN FETCH tc.coachType ct
      WHERE i.journey.journeyId = :journeyId
      ORDER BY ct.typeCode, i.quotaType
      """)
  List<JourneySeatInventoryEntity> findByJourneyId(@Param("journeyId") Long journeyId);

  // Single row lookup — used during booking
  @Query("""
      SELECT i FROM JourneySeatInventoryEntity i
      WHERE i.journey.journeyId = :journeyId
        AND i.trainCoach.coachId = :coachId
        AND i.quotaType = :quotaType
      """)
  Optional<JourneySeatInventoryEntity> findByJourneyCoachQuota(
    @Param("journeyId")  Long      journeyId,
    @Param("coachId")    Long      coachId,
    @Param("quotaType")  QuotaType quotaType);

  // Check if inventory already exists for a journey (prevent duplicate init)
  boolean existsByJourney_JourneyId(Long journeyId);

  // All GENERAL rows for a journey — used for waitlist cancellation job
  @Query("""
      SELECT i FROM JourneySeatInventoryEntity i
      WHERE i.journey.journeyId = :journeyId
        AND i.quotaType = 'GENERAL'
      """)
  List<JourneySeatInventoryEntity> findGeneralByJourneyId(@Param("journeyId") Long journeyId);

  // All inventory rows for a coach from a given date onwards
  @Query("""
      SELECT i FROM JourneySeatInventoryEntity i
      JOIN FETCH i.journey j
      WHERE i.trainCoach.coachId = :coachId
        AND j.journeyDate >= :fromDate
      ORDER BY j.journeyDate ASC
      """)
  List<JourneySeatInventoryEntity> findByCoachFromDate(
    @Param("coachId")  Long      coachId,
    @Param("fromDate") LocalDate fromDate);

  // Delete unbooked inventory rows for a coach within a date range
  // An inventory row is "unbooked" when all booked counters are zero
  @Modifying
  @Query("""
      DELETE FROM JourneySeatInventoryEntity i
      WHERE i.trainCoach.coachId = :coachId
        AND i.journey.journeyDate >= :fromDate
        AND (:toDate IS NULL OR i.journey.journeyDate <= :toDate)
        AND i.bookedConfirmed = 0
        AND i.bookedRac = 0
        AND i.bookedWaitlist = 0
      """)
  int deleteUnbookedInventory(
    @Param("coachId")  Long      coachId,
    @Param("fromDate") LocalDate fromDate,
    @Param("toDate")   LocalDate toDate);

  // Update GENERAL inventory totals for a coach within a date range
  @Modifying
  @Query("""
      UPDATE JourneySeatInventoryEntity i
      SET i.totalSeats = :totalSeats,
          i.totalRac = :totalRac,
          i.waitlistLimit = :waitlistLimit
      WHERE i.trainCoach.coachId = :coachId
        AND i.quotaType = 'GENERAL'
        AND i.journey.journeyDate >= :fromDate
        AND (:toDate IS NULL OR i.journey.journeyDate <= :toDate)
      """)
  int updateGeneralInventory(
    @Param("coachId")        Long      coachId,
    @Param("totalSeats")     int       totalSeats,
    @Param("totalRac")       int       totalRac,
    @Param("waitlistLimit")  int       waitlistLimit,
    @Param("fromDate")       LocalDate fromDate,
    @Param("toDate")         LocalDate toDate);

  // Update TATKAL inventory totals for a coach within a date range
  @Modifying
  @Query("""
      UPDATE JourneySeatInventoryEntity i
      SET i.totalSeats = :totalSeats
      WHERE i.trainCoach.coachId = :coachId
        AND i.quotaType = 'TATKAL'
        AND i.journey.journeyDate >= :fromDate
        AND (:toDate IS NULL OR i.journey.journeyDate <= :toDate)
      """)
  int updateTatkalInventory(
    @Param("coachId")    Long      coachId,
    @Param("totalSeats") int       totalSeats,
    @Param("fromDate")   LocalDate fromDate,
    @Param("toDate")     LocalDate toDate);
}
