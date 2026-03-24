package com.railway.main_service.repository;

import com.railway.main_service.entity.CoachTypeEntity;
import com.railway.main_service.entity.TrainTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoachTypeRepository extends JpaRepository<CoachTypeEntity, Long> {

  boolean existsByTypeCode(String typeCode);

  boolean existsByTypeName(String typeName);

  boolean existsByTypeNameAndTypeCodeNot(String typeName, String typeCode);

  Optional<CoachTypeEntity> findByTypeCode(String typeCode);

  @Query("SELECT c FROM CoachTypeEntity c " +
    "WHERE c.effectiveFrom <= CURRENT_DATE AND (c.effectiveTill IS NULL OR c.effectiveTill > CURRENT_DATE) " +
    "ORDER BY c.typeCode ASC")
  List<CoachTypeEntity> findAllActiveOrderByTypeCodeAsc();


  @Query("""
SELECT c FROM CoachTypeEntity c
WHERE c.effectiveFrom <= CURRENT_DATE AND (c.effectiveTill IS NULL OR c.effectiveTill > CURRENT_DATE)
AND (
  :search IS NULL OR
  LOWER(c.typeCode) LIKE LOWER(CONCAT(CAST(:search AS string), '%')) OR
  LOWER(c.typeName) LIKE LOWER(CONCAT(CAST(:search AS string), '%'))
)
ORDER BY c.typeCode ASC
""")
  List<CoachTypeEntity> findActiveForDropdown(@Param("search") String search);


  @Query("SELECT c FROM CoachTypeEntity c WHERE " +
    "(:search IS NULL OR LOWER(c.typeCode) LIKE LOWER(CONCAT(CAST(:search AS string), '%')) " +
    "OR LOWER(c.typeName) LIKE LOWER(CONCAT(CAST(:search AS string), '%'))) " +
    "ORDER BY c.typeCode ASC")
  List<CoachTypeEntity> findAllForAdmin(@Param("search") String search);

  @Query("SELECT c FROM CoachTypeEntity c " +
    "WHERE c.typeId IN :typeIds " +
    "AND c.effectiveFrom <= CURRENT_DATE AND (c.effectiveTill IS NULL OR c.effectiveTill > CURRENT_DATE)")
  List<CoachTypeEntity> findAllActiveByTypeIdIn(@Param("typeIds") List<Long> typeIds);
}
