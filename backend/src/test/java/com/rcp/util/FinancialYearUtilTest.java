package com.rcp.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialYearUtilTest {

    @Test
    void aprilBelongsToTheYearItStartsIn() {
        assertThat(FinancialYearUtil.financialYearOf(LocalDate.of(2026, 4, 1))).isEqualTo("2026-27");
    }

    @Test
    void marchOfTheFollowingCalendarYearStillBelongsToTheSameFinancialYear() {
        assertThat(FinancialYearUtil.financialYearOf(LocalDate.of(2027, 3, 31))).isEqualTo("2026-27");
    }

    @Test
    void januaryBelongsToThePreviousFinancialYear() {
        assertThat(FinancialYearUtil.financialYearOf(LocalDate.of(2026, 1, 15))).isEqualTo("2025-26");
    }
}
