package com.railway.main_service.dto.request.station;

import com.railway.main_service.utility.excel.ExcelColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationExcelDto {

  @ExcelColumn(value = "station_code", order = 0, required = true)
  private String stationCode;

  @ExcelColumn(value = "station_name", order = 1, required = true)
  private String stationName;

  @ExcelColumn(value = "city", order = 2, required = true)
  private String city;

  @ExcelColumn(value = "state", order = 3, required = true)
  private String state;

  @ExcelColumn(value = "latitude", order = 4, required = false)
  private Double latitude;

  @ExcelColumn(value = "longitude", order = 5, required = false)
  private Double longitude;

  @ExcelColumn(value = "zone", order = 6, required = true)
  private String zone;

  @ExcelColumn(value = "is_junction", order = 7, required = true)
  private Boolean isJunction;

  @ExcelColumn(value = "num_platforms", order = 8, required = true)
  private Integer numPlatforms;

  @ExcelColumn(value = "is_active", order = 9, required = false)
  private Boolean isActive;
}
