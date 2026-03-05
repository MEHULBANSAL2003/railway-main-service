package com.railway.main_service.constants;

public class ApiConstants {
  public static final String API_BASE = "/api/main";

  public static final String STATIONS = API_BASE + "/stations";


  public static final String STATES = API_BASE + "/states";
  public static final String CITIES = API_BASE + "/cities";
  public static final String ZONES = API_BASE + "/zones";
  public static final String TRAIN_TYPES = API_BASE + "/train-types";

  //stations
  public static final String ADD_NEW_STATION = "/admin/add/new/station";
  public static final String UPLOAD_STATIONS_EXCEL = "/upload/excel";
  public static final String GET_STATIONS = "/get/all/list";
  public static final String SET_ACTIVE_INACTIVE = "/set/active/inactive/{stationCode}";
  public static final String UPDATE_STATION_DETAILS = "/update/details/{stationCode}";
  public static final String DELETE_STATION = "/delete/{stationCode}";
  public static final String GET_ALL_PERMANENTLY_DELETED_STATIONS = "/get/all/permanent/deleted";
  public static final String RESTORE_DELETED_STATION = "/restore/{stationCode}";

  //states
  public static final String UPLOAD_STATES_DATA_BY_EXCEL = "/upload/excel";
  public static final String GET_STATES = "/get/all/list";

  //zones
  public static final String ADD_ZONE = "/add";
  public static final String GET_ZONES = "/get/all";

  //cities
  public static final String ADD_CITY = "/add/new";
  public static final String GET_CITIES = "/get/all";
  public static final String UPLOAD_CITIES_EXCEL = "/upload/excel";
  public static final String CITIES_BY_STATE_NAME = "/by/state/name";

// train types
  public static final String ADD_TRAIN_TYPE = "/add/new";
  public static final String GET_TRAIN_TYPES = "/get/all";
 public static final String UPDATE_TRAIN_TYPE = "/update/{typeCode}";
 public static final String CHANGE_STATUS = "/change/status/{typeCode}";


  private ApiConstants() {
    throw new IllegalStateException("Constants class");
  }
}
