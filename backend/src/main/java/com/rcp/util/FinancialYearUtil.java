package com.rcp.util;

import java.time.LocalDate;
import java.time.Month;

// Computes the Indian financial-year label ("2026-27") used inside application numbers.
// The FY runs April (inclusive) to March (inclusive of the following calendar year).
public final class FinancialYearUtil {

    private FinancialYearUtil() {
    }

    // Examples: any date in Apr 2026..Mar 2027 -> "2026-27". A date in Jan 2026 -> "2025-26".
    public static String financialYearOf(LocalDate date) {
        int startYear = date.getMonth().compareTo(Month.APRIL) >= 0 ? date.getYear() : date.getYear() - 1;
        int endYearShort = (startYear + 1) % 100;
        return String.format("%d-%02d", startYear, endYearShort);
    }
}
