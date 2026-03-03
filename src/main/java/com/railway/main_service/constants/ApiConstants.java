package com.railway.main_service.constants;

public class ApiConstants {
  public static final String API_BASE = "/api/main";

  public static final String STATIONS = API_BASE + "/stations";

  public static final String TRAINS = API_BASE + "/trains";

  public static final String ROUTES = API_BASE + "/routes";
  public static final String STATES = API_BASE + "/states";
  public static final String CITIES = API_BASE + "/cities";
  public static final String ZONES = API_BASE + "/zones";

  public static final String ADD_NEW_STATION = "/admin/add/new/station";
  public static final String UPLOAD_STATIONS_EXCEL = "/upload/excel";
  public static final String GET_STATIONS = "/get/all/list";
  public static final String SEARCH_STATIONS = "/search/by/name";
  public static final String SET_ACTIVE_INACTIVE = "/set/active/inactive/{stationCode}";
  public static final String UPDATE_STATION_DETAILS = "/update/details/{stationCode}";


    public static final String UPLOAD_STATES_DATA_BY_EXCEL = "/upload/excel";
  public static final String GET_STATES = "/get/all/list";

  public static final String ADD_ZONE = "/add";
  public static final String GET_ZONES = "/get/all";

  public static final String ADD_CITY = "/add/new";
  public static final String GET_CITIES = "/get/all";
  public static final String UPLOAD_CITIES_EXCEL = "/upload/excel";
  public static final String CITIES_BY_STATE_NAME = "/by/state/name";
  public static final String GET_ALL_CITIES = "/get/all/list";




  private ApiConstants() {
    throw new IllegalStateException("Constants class");
  }
}
