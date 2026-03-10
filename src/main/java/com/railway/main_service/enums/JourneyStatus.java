package com.railway.main_service.enums;

public enum JourneyStatus {
  SCHEDULED,   // future, or today before departure
  DEPARTED,    // today, after source departure time
  COMPLETED,   // past journey date
  CANCELLED    // admin manually cancelled
}
