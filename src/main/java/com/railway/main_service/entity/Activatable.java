package com.railway.main_service.entity;

import com.railway.main_service.utility.EffectiveDateUtils;

import java.time.LocalDate;

public interface Activatable {

  LocalDate getEffectiveFrom();

  LocalDate getEffectiveTill();

  String getReason();

  default boolean isCurrentlyActive() {
    return EffectiveDateUtils.isCurrentlyActive(getEffectiveFrom(), getEffectiveTill());
  }
}
