package com.railway.main_service.utility;

import java.time.LocalDate;

public final class EffectiveDateUtils {

  private EffectiveDateUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Checks if a date-range is currently active (as of today).
   * Formula: effectiveFrom <= today AND (effectiveTill == null OR effectiveTill > today)
   * effectiveTill is exclusive — it is the first day of inactivity.
   */
  public static boolean isCurrentlyActive(LocalDate effectiveFrom, LocalDate effectiveTill) {
    return isActiveOn(effectiveFrom, effectiveTill, LocalDate.now());
  }

  /**
   * Checks if a date-range is active on a specific date.
   * Formula: effectiveFrom <= date AND (effectiveTill == null OR effectiveTill > date)
   */
  public static boolean isActiveOn(LocalDate effectiveFrom, LocalDate effectiveTill, LocalDate date) {
    if (effectiveFrom == null || date == null) return false;
    return !effectiveFrom.isAfter(date)
      && (effectiveTill == null || effectiveTill.isAfter(date));
  }
}
