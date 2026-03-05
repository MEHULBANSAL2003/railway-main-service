package com.railway.main_service.repository;


import com.railway.main_service.entity.TrainTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainTypeRepository extends JpaRepository<TrainTypeEntity, Long> {

  boolean existsByTypeCode(String typeCode);

  boolean existsByTypeName(String typeName);

  boolean existsByTypeNameAndTypeCodeNot(String typeName, String typeCode);

  Optional<TrainTypeEntity> findByTypeCode(String typeCode);

  // Admin view — all with optional search
  @Query("SELECT t FROM TrainTypeEntity t WHERE " +
    "(:search IS NULL OR LOWER(t.typeCode) LIKE LOWER(CONCAT(:search, '%')) " +
    "OR LOWER(t.typeName) LIKE LOWER(CONCAT(:search, '%')))" +
    "ORDER BY t.typeCode ASC")
  List<TrainTypeEntity> findAllForAdmin(@Param("search") String search);

  // Dropdown — active only
  List<TrainTypeEntity> findAllByIsActiveTrueOrderByTypeCodeAsc();
}
