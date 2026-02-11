package com.railway.main_service.constants;

public class ApiConstants {
  public static final String API_BASE = "/api";

  public static final String STATIONS = API_BASE + "/stations";

  public static final String TRAINS = API_BASE + "/trains";

  public static final String ROUTES = API_BASE + "/routes";

  public static final String ADD_NEW_STATION = "/admin/add/new/station";
  public static final String GET_STATIONS = "/get/all/list";





  private ApiConstants() {
    throw new IllegalStateException("Constants class");
  }
}
