package com.railway.main_service.repository;

import com.railway.main_service.entity.TrainEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainRepository extends JpaRepository<TrainEntity, Long> {

  // ── Existence checks ──────────────────────────────────────────────────────
  boolean existsByTrainNumber(String trainNumber);
  boolean existsByTrainName(String trainName);
  boolean existsByTrainNameAndTrainIdNot(String trainName, Long trainId);

  // ── Single fetch ──────────────────────────────────────────────────────────
  Optional<TrainEntity> findByTrainNumber(String trainNumber);

  // ── Paginated admin table with filters + sorting ──────────────────────────
  // Sorting is handled by Pageable — Spring Data applies ORDER BY automatically.
  // All filter params are optional — null means skip that filter.
  // isActive: null = all, true = active only, false = inactive only
  // NOTE: JOIN FETCH cannot be used with Page queries — Spring would throw
  //       "HHH90003004: firstResult/maxResults specified with collection fetch".
  //       Use regular JOIN instead and accept N+1 prevention via @EntityGraph or
  //       a separate countQuery. We use a separate countQuery below.
  @Query(
    value = "SELECT t FROM TrainEntity t " +
      "JOIN t.trainType tt " +
      "JOIN t.zone z " +
      "WHERE (:trainTypeCode IS NULL OR tt.typeCode = CAST(:trainTypeCode AS string)) " +
      "AND   (:zoneCode      IS NULL OR z.code      = CAST(:zoneCode      AS string)) " +
      "AND   (:isActive      IS NULL OR t.isActive  = :isActive) " +
      "AND   (:search        IS NULL " +
      "    OR LOWER(t.trainNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
      "    OR LOWER(t.trainName)   LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))",
    countQuery =
      "SELECT COUNT(t) FROM TrainEntity t " +
        "JOIN t.trainType tt " +
        "JOIN t.zone z " +
        "WHERE (:trainTypeCode IS NULL OR tt.typeCode = CAST(:trainTypeCode AS string)) " +
        "AND   (:zoneCode      IS NULL OR z.code      = CAST(:zoneCode      AS string)) " +
        "AND   (:isActive      IS NULL OR t.isActive  = :isActive) " +
        "AND   (:search        IS NULL " +
        "    OR LOWER(t.trainNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
        "    OR LOWER(t.trainName)   LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))"
  )
  Page<TrainEntity> findAllForAdminPaged(
    @Param("trainTypeCode") String trainTypeCode,
    @Param("zoneCode")      String zoneCode,
    @Param("isActive")      Boolean isActive,
    @Param("search")        String search,
    Pageable pageable
  );

  // ── Dropdown — active trains only, no pagination ──────────────────────────
  @Query("SELECT t FROM TrainEntity t " +
    "JOIN FETCH t.trainType tt " +
    "JOIN FETCH t.zone z " +
    "WHERE t.isActive = true " +
    "AND (:search IS NULL " +
    "  OR LOWER(t.trainNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
    "  OR LOWER(t.trainName)   LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
    "ORDER BY t.trainNumber ASC")
  List<TrainEntity> findActiveForDropdown(@Param("search") String search);

  // ── Cascade checks ────────────────────────────────────────────────────────
  int countByTrainType_TypeCodeAndIsActiveTrue(String typeCode);

  @Query("SELECT COUNT(t) FROM TrainEntity t " +
    "JOIN t.zone z " +
    "WHERE z.code = :zoneCode AND t.isActive = true")
  int countActiveTrainsByZoneCode(@Param("zoneCode") String zoneCode);
}
