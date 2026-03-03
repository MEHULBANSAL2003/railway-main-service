package com.railway.main_service.specification;

import com.railway.main_service.dto.request.station.StationFilterRequest;
import com.railway.main_service.entity.StationEntity;
import com.railway.main_service.enums.StationType;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class StationSpecification {

  private StationSpecification() {}

  // Normal list — excludes permanently deleted
  public static Specification<StationEntity> build(StationFilterRequest filter) {
    return buildInternal(filter, false);
  }

  // Deleted list — only permanently deleted
  public static Specification<StationEntity> buildDeleted(StationFilterRequest filter) {
    return buildInternal(filter, true);
  }

  // Core — private, onlyDeleted flips the isPermanentlyDeleted predicate
  private static Specification<StationEntity> buildInternal(StationFilterRequest filter, boolean onlyDeleted) {
    return (root, query, cb) -> {

      if (query.getResultType() != Long.class && query.getResultType() != long.class) {
        root.fetch("city", JoinType.LEFT).fetch("state", JoinType.LEFT);
        root.fetch("zone", JoinType.LEFT);
      }

      Join<?, ?> city  = root.join("city",  JoinType.LEFT);
      Join<?, ?> state = city.join("state", JoinType.LEFT);
      Join<?, ?> zone  = root.join("zone",  JoinType.LEFT);

      List<Predicate> predicates = new ArrayList<>();

      predicates.add(cb.equal(root.get("isPermanentlyDeleted"), onlyDeleted));

      if (hasValue(filter.getSearchTerm())) {
        String pattern = filter.getSearchTerm().trim().toLowerCase() + "%";
        predicates.add(cb.or(
          cb.like(cb.lower(root.get("stationCode")), pattern),
          cb.like(cb.lower(root.get("stationName")), pattern),
          cb.like(cb.lower(city.get("name")),        pattern),
          cb.like(cb.lower(state.get("name")),       pattern),
          cb.like(cb.lower(zone.get("name")),        pattern)
        ));
      }

      if (hasValue(filter.getState()))
        predicates.add(cb.equal(cb.lower(state.get("name")), filter.getState().trim().toLowerCase()));

      if (hasValue(filter.getZone()))
        predicates.add(cb.equal(cb.lower(zone.get("code")), filter.getZone().trim().toLowerCase()));

      if (hasValue(filter.getStationType()))
        predicates.add(cb.equal(root.get("stationType"),
          StationType.valueOf(filter.getStationType().trim().toUpperCase())));

      if (filter.getIsActive() != null)
        predicates.add(cb.equal(root.get("isActive"), filter.getIsActive()));

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static boolean hasValue(String s) {
    return s != null && !s.isBlank();
  }
}
