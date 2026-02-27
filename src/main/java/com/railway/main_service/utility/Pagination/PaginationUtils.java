package com.railway.main_service.utility.Pagination;


import com.railway.main_service.dto.request.Pagination.PageRequestDto;
import com.railway.main_service.dto.response.pagination.PageResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PaginationUtils {

  // Maps API sortBy field names → actual JPQL paths for StationEntity
  public static final Map<String, String> STATION_SORT_FIELDS = Map.of(
    "id",           "id",
    "stationCode",  "stationCode",
    "stationName",  "stationName",
    "numPlatforms", "numPlatforms",
    "stationType",  "stationType",
    "isActive",     "isActive",
    "createdAt",    "createdAt",
    // Related entity fields → mapped to their join alias paths
    "cityName",     "city.name",
    "stateName",    "city.state.name",
    "zoneName",     "zone.name"
  );

  public static Pageable createPageable(PageRequestDto pageRequest) {
    Sort sort = pageRequest.getSortDirection().equalsIgnoreCase("DESC")
      ? Sort.by(pageRequest.getSortBy()).descending()
      : Sort.by(pageRequest.getSortBy()).ascending();

    return PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), sort);
  }

  // Use this for entities with JOIN FETCH (stations, etc.)
  public static Pageable createPageable(PageRequestDto pageRequest, Map<String, String> sortFieldMap) {
    String requestedSort = pageRequest.getSortBy();

    // Resolve to actual JPQL path, fallback to "id" if unknown
    String resolvedSort = sortFieldMap.getOrDefault(requestedSort, "id");

    Sort sort = pageRequest.getSortDirection().equalsIgnoreCase("DESC")
      ? Sort.by(resolvedSort).descending()
      : Sort.by(resolvedSort).ascending();

    return PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), sort);
  }

  public static <E, D> PageResponseDto<D> toPageResponse(Page<E> page, Function<E, D> mapper) {
    List<D> content = page.getContent()
      .stream()
      .map(mapper)
      .collect(Collectors.toList());

    return PageResponseDto.<D>builder()
      .content(content)
      .pageNumber(page.getNumber())
      .pageSize(page.getSize())
      .totalElements(page.getTotalElements())
      .totalPages(page.getTotalPages())
      .first(page.isFirst())
      .last(page.isLast())
      .empty(page.isEmpty())
      .numberOfElements(page.getNumberOfElements())
      .build();
  }
}
