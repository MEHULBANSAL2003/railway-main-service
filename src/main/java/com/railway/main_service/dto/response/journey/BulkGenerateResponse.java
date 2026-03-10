package com.railway.main_service.dto.response.journey;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BulkGenerateResponse {
  private int             created;
  private int             skipped;    // already existed or not a scheduled day
  private int             total;      // total days checked (120)
  private LocalDate       from;
  private LocalDate       to;
  private List<LocalDate> createdDates;  // which dates were actually created
}
