package com.railway.main_service.constants;

public class ApiConstants {
  public static final String API_BASE = "/api/main";

  public static final String ZONE_BASE_V1 = API_BASE + "/v1/zones";


  //zones
  public static final String GET_UPDATE_ZONE_DETAIL = "/{zoneId}";
  public static final String DEACTIVATE_ZONE = "/{zoneId}/deactivate";
  public static final String REACTIVATE_ZONE = "/{zoneId}/reactivate";
  public static final String UPLOAD_ZONE_EXCEL = "/upload/excel";

  private ApiConstants() {
    throw new IllegalStateException("Constants class");
  }
}
