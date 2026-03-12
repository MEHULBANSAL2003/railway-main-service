package com.railway.main_service.excel;

import com.railway.common.excel.ExcelRowMapper;
import com.railway.common.excel.ExcelUtils;
import com.railway.main_service.dto.request.zone.CreateZoneRequest;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ZoneExcelRowMapper implements ExcelRowMapper<CreateZoneRequest> {

  @Override
  public CreateZoneRequest map(Row row, int rowNumber) {
    String zoneName = ExcelUtils.getString(row, 0);
    String zoneCode = ExcelUtils.getString(row, 1);
    String effectiveFrom = ExcelUtils.getString(row, 2);

    if (zoneName == null) throw new IllegalArgumentException("Zone name is required");
    if (zoneCode == null) throw new IllegalArgumentException("Zone code is required");
    if (effectiveFrom == null) throw new IllegalArgumentException("Effective from is required");

    CreateZoneRequest request = new CreateZoneRequest();
    request.setZoneName(zoneName);
    request.setZoneCode(zoneCode);
    try {
      request.setEffectiveFrom(LocalDate.parse(effectiveFrom));
    } catch (Exception e) {
      throw new IllegalArgumentException("Effective from must be in format YYYY-MM-DD");
    }

    return request;
  }

  @Override
  public String[] expectedHeaders() {
    return new String[]{"Zone Name", "Zone Code", "Effective From"};
  }
}
