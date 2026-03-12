package com.railway.main_service.specification;

import com.railway.common.pagination.BaseSpecification;
import com.railway.common.pagination.FilterRequest;
import com.railway.main_service.entity.ZoneEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

public class ZoneSpecification extends BaseSpecification<ZoneEntity> {

  public static Specification<ZoneEntity> withFilters(FilterRequest request) {
    return (root, query, cb) -> {
      BaseSpecification<ZoneEntity> spec = new BaseSpecification<>();
      List<Predicate> predicates = spec.newPredicateList();

      predicates.add(spec.activeAsOf(root, cb, request.getAsOfDate()));

      if (hasFilter(request, "zoneName"))
        predicates.add(spec.containsIgnoreCase(root, cb, "zoneName",
          getFilter(request, "zoneName")));

      if (hasFilter(request, "zoneCode"))
        predicates.add(spec.equalsIgnoreCase(root, cb, "zoneCode",
          getFilter(request, "zoneCode")));

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
