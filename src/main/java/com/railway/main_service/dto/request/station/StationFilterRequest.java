package com.railway.main_service.dto.request.station;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Extended page request for station list with search + filter support.
 * Replaces bare PageRequestDto on the GET /stations endpoint.
 */
@Data
public class StationFilterRequest {

  // ── Pagination ───────────────────────────────────────────
  @Min(value = 0, message = "Page number must be 0 or greater")
  private int page = 0;

  @Min(value = 1)
  @Max(value = 100)
  private int size = 20;

  // ── Sort ─────────────────────────────────────────────────
  private String sortBy        = "stationId";
  private String sortDirection = "ASC";

  // ── Search / Filters (all optional) ─────────────────────
  private String searchTerm;   // searches code, name, city, state, zone (LIKE prefix%)
  private String state;        // filter by state name  (exact, case-insensitive)
  private String zone;         // filter by zone code   (exact, case-insensitive)
  private String stationType;  // REGULAR | JUNCTION | TERMINUS | HALT
  private Boolean isActive;    // true | false
}
