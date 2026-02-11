package com.railway.main_service.dto.request.Pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestDto {

  @Min(value = 0, message = "Page number must be 0 or greater")
  private int page = 0;  // Default to first page

  @Min(value = 1, message = "Page size must be at least 1")
  @Max(value = 100, message = "Page size cannot exceed 100")
  private int size = 10;  // Default page size

  private String sortBy = "id";  // Default sort field

  private String sortDirection = "ASC";  // ASC or DESC

}
