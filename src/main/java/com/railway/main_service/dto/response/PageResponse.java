package com.railway.main_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {
  private List<T> content;
  private long    totalElements;
  private int     totalPages;
  private int     currentPage;  // 1-based
  private int     pageSize;
  private boolean first;
  private boolean last;
}
