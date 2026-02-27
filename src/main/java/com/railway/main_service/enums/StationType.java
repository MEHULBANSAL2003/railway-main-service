package com.railway.main_service.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StationType {

  JUNCTION("Junction"),
  TERMINAL("Terminal"),
  CENTRAL("Central"),
  REGULAR("Regular"),
  HALT("Halt"),
  CANTT("Cantonment");

  private final String displayName;
}
