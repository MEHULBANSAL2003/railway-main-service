package com.railway.main_service.repository;

import com.railway.main_service.entity.JourneySeatInventoryEntity;
import com.railway.main_service.enums.QuotaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
