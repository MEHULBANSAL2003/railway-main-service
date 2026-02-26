package com.railway.main_service.repository;


import com.railway.main_service.entity.StateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StateRepository extends JpaRepository<StateEntity, Long> {

  Optional<StateEntity> findByCode(String code);

  Optional<StateEntity> findByName(String name);

  List<StateEntity> findAllByIsActiveTrueOrderByName();

  boolean existsByCode(String code);

  boolean existsByName(String name);
}
