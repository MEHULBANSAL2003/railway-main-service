package com.railway.main_service.repository;

import com.railway.main_service.entity.CoachTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CoachTypeRepository extends JpaRepository<CoachTypeEntity, Long> {

  boolean existsByTypeCode(String typeCode);

  boolean existsByTypeName(String typeName);

  boolean existsByTypeNameAndTypeCodeNot(String typeName, String typeCode);

  Optional<CoachTypeEntity> findByTypeCode(String typeCode);

  // Active coach types on today — period-based (replaces findAllByIsActiveTrueOrderByTypeCodeAsc)
  @Query("""
    SELECT c FROM CoachTypeEntity c
    WHERE EXISTS (
      SELECT 1 FROM CoachTypePeriodEntity p
      WHERE p.coachType.typeId = c.typeId
        AND p.effectiveFrom <= :today
        AND (p.effectiveTill IS NULL OR p.effectiveTill >= :today)
    )
    ORDER BY c.typeCode ASC
    """)
  List<CoachTypeEntity> findActiveOnDate(@Param("today") LocalDate today);

  // Dropdown — active coach types with optional search
  @Query("""
    SELECT c FROM CoachTypeEntity c
    WHERE EXISTS (
      SELECT 1 FROM CoachTypePeriodEntity p
      WHERE p.coachType.typeId = c.typeId
        AND p.effectiveFrom <= :today
        AND (p.effectiveTill IS NULL OR p.effectiveTill >= :today)
    )
    AND (
      :search IS NULL OR
      LOWER(c.typeCode) LIKE LOWER(CONCAT(CAST(:search AS string), '%')) OR
      LOWER(c.typeName) LIKE LOWER(CONCAT(CAST(:search AS string), '%'))
    )
    ORDER BY c.typeCode ASC
    """)
  List<CoachTypeEntity> findActiveForDropdown(@Param("search") String search, @Param("today") LocalDate today);

  // Admin view — all with optional search
  @Query("SELECT c FROM CoachTypeEntity c WHERE " +
    "(:search IS NULL OR LOWER(c.typeCode) LIKE LOWER(CONCAT(CAST(:search AS string), '%')) " +
    "OR LOWER(c.typeName) LIKE LOWER(CONCAT(CAST(:search AS string), '%'))) " +
    "ORDER BY c.typeCode ASC")
  List<CoachTypeEntity> findAllForAdmin(@Param("search") String search);

  // Find active coach types by IDs — period-based (replaces findAllByTypeIdInAndIsActiveTrue)
  @Query("""
    SELECT c FROM CoachTypeEntity c
    WHERE c.typeId IN :typeIds
    AND EXISTS (
      SELECT 1 FROM CoachTypePeriodEntity p
      WHERE p.coachType.typeId = c.typeId
        AND p.effectiveFrom <= :today
        AND (p.effectiveTill IS NULL OR p.effectiveTill >= :today)
    )
    """)
  List<CoachTypeEntity> findAllByTypeIdInAndActiveOnDate(
    @Param("typeIds") List<Long> typeIds,
    @Param("today")   LocalDate today);
}
