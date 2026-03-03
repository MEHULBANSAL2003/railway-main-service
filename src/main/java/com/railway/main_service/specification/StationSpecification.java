package com.railway.main_service.specification;

import com.railway.main_service.dto.request.station.StationFilterRequest;
import com.railway.main_service.entity.StationEntity;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification for dynamic station filtering.
 *
 * Joins (all LEFT so unmatched rows are never excluded unintentionally):
 *   station → city → state
 *   station → zone
 *
 * Every predicate is optional — only added when the filter value is non-null/non-blank.
 */
public class StationSpecification {

  private StationSpecification() {}

  public static Specification<StationEntity> build(StationFilterRequest filter) {
    return (root, query, cb) -> {

      // ── Fetch joins (avoids N+1, same as your existing JPQL query) ──
      // Only add fetch joins on the main query, not count query
      if (query.getResultType() != Long.class && query.getResultType() != long.class) {
        root.fetch("city", JoinType.LEFT)
          .fetch("state", JoinType.LEFT);
        root.fetch("zone", JoinType.LEFT);
      }

      // Regular joins for WHERE predicates
      Join<?, ?> city  = root.join("city",  JoinType.LEFT);
      Join<?, ?> state = city.join("state", JoinType.LEFT);
      Join<?, ?> zone  = root.join("zone",  JoinType.LEFT);

      List<Predicate> predicates = new ArrayList<>();

      predicates.add(cb.equal(root.get("isPermanentlyDeleted"), false));

      // ── searchTerm: prefix LIKE on code, name, city, state, zone ──
      if (hasValue(filter.getSearchTerm())) {
        String pattern = filter.getSearchTerm().trim().toLowerCase() + "%";
        predicates.add(cb.or(
          cb.like(cb.lower(root.get("stationCode")), pattern),
          cb.like(cb.lower(root.get("stationName")), pattern),
          cb.like(cb.lower(city.get("name")),         pattern),
          cb.like(cb.lower(state.get("name")),        pattern),
          cb.like(cb.lower(zone.get("name")),         pattern)
        ));
      }

      // ── state (by name, case-insensitive) ──
      if (hasValue(filter.getState())) {
        predicates.add(
          cb.equal(cb.lower(state.get("name")), filter.getState().trim().toLowerCase())
        );
      }

      // ── zone (by code, case-insensitive) ──
      if (hasValue(filter.getZone())) {
        predicates.add(
          cb.equal(cb.lower(zone.get("code")), filter.getZone().trim().toLowerCase())
        );
      }

      // ── stationType (exact enum match) ──
      if (hasValue(filter.getStationType())) {
        predicates.add(
          cb.equal(root.get("stationType"),
            com.railway.main_service.enums.StationType
              .valueOf(filter.getStationType().trim().toUpperCase()))
        );
      }

      // ── isActive ──
      if (filter.getIsActive() != null) {
        predicates.add(cb.equal(root.get("isActive"), filter.getIsActive()));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static boolean hasValue(String s) {
    return s != null && !s.isBlank();
  }
}
