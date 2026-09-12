package com.rcp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Small helper so every place that needs to round money does it the same, explicit way.
// The spec is emphatic that money must never touch double/float - everything here is BigDecimal in, BigDecimal out.
public final class MoneyUtil {

    private MoneyUtil() {
    }

    // Rounds to the nearest whole currency unit (0 decimal places), ties rounding away from zero -
    // i.e. exactly what "round_half_up" means for positive amounts (all amounts here are non-negative).
    public static BigDecimal roundHalfUpToWhole(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    // A percentage-of-amount helper: amount * percent / 100, rounded half-up to a whole currency unit.
    // Used for both the urgency surcharge and the percentage-based leg of the security deposit -
    // see FeeCalculationService for why rounding happens at this point rather than only at the total.
    public static BigDecimal percentOfRoundedToWhole(BigDecimal amount, BigDecimal percent) {
        BigDecimal raw = amount.multiply(percent).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        return roundHalfUpToWhole(raw);
    }
}
