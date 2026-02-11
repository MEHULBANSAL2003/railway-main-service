package com.railway.main_service.utility.Pagination;


import com.railway.main_service.dto.request.Pagination.PageRequestDto;
import com.railway.main_service.dto.response.pagination.PageResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PaginationUtils {

  /**
   * Creates a Pageable object from PageRequestDto
   *
   * @param pageRequest - Contains page, size, sortBy, sortDirection
   * @return Pageable object for repository queries
   */
  public static Pageable createPageable(PageRequestDto pageRequest) {
    Sort sort = pageRequest.getSortDirection().equalsIgnoreCase("DESC")
      ? Sort.by(pageRequest.getSortBy()).descending()
      : Sort.by(pageRequest.getSortBy()).ascending();

    return PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), sort);
  }

  /**
   * Converts Spring Data Page to custom PageResponseDto
   *
   * @param page - Spring Data Page object
   * @param mapper - Function to convert Entity to DTO
   * @return PageResponseDto with converted content
   */
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
