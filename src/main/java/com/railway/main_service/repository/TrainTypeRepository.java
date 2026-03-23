package com.railway.main_service.repository;

import com.railway.main_service.entity.TrainTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainTypeRepository extends JpaRepository<TrainTypeEntity, Long> {

  boolean existsByTypeCode(String typeCode);
  boolean existsByTypeName(String typeName);
  boolean existsByTypeNameAndTypeCodeNot(String typeName, String typeCode);
  Optional<TrainTypeEntity> findByTypeCode(String typeCode);

  @Query("SELECT t FROM TrainTypeEntity t WHERE " +
    "(:search IS NULL OR LOWER(t.typeCode) LIKE LOWER(CONCAT(CAST(:search AS string), '%')) " +
    "OR LOWER(t.typeName) LIKE LOWER(CONCAT(CAST(:search AS string), '%'))) " +
    "ORDER BY t.typeCode ASC")
  List<TrainTypeEntity> findAllForAdmin(@Param("search") String search);

  // Dropdown — only train types that have an active period covering today
  @Query("""
    SELECT t FROM TrainTypeEntity t
    WHERE EXISTS (
      SELECT 1 FROM TrainTypePeriodEntity p
      WHERE p.trainType.typeId = t.typeId
        AND p.effectiveFrom <= :today
        AND (p.effectiveTill IS NULL OR p.effectiveTill >= :today)
    )
    AND (:search IS NULL
      OR LOWER(t.typeCode) LIKE LOWER(CONCAT(CAST(:search AS string), '%'))
      OR LOWER(t.typeName) LIKE LOWER(CONCAT(CAST(:search AS string), '%')))
    ORDER BY t.typeCode ASC
    """)
  List<TrainTypeEntity> findActiveForDropdown(@Param("search") String search, @Param("today") LocalDate today);
}
