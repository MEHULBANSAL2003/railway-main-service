package com.railway.main_service.constants;

public class ApiConstants {
  public static final String API_BASE = "/api/train";

  public static final String SEARCH = API_BASE + "/search";
  public static final String STATIONS = API_BASE + "/stations";
  public static final String ROUTES = API_BASE + "/routes";

  public static final String USER_BASE = API_BASE + "/user";
  public static final String USER_BOOKINGS = USER_BASE + "/bookings";
  public static final String USER_FAVORITES = USER_BASE + "/favorites";

  // Admin endpoints (ADMIN role required)
  public static final String ADMIN_BASE = API_BASE + "/admin";
  public static final String ADMIN_TRAINS = ADMIN_BASE + "/trains";
  public static final String ADMIN_STATIONS = ADMIN_BASE + "/stations";
  public static final String ADMIN_SCHEDULES = ADMIN_BASE + "/schedules";


  private ApiConstants() {
    throw new IllegalStateException("Constants class");
  }
}
