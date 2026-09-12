package com.rcp.calculation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rcp.domain.ApplicantType;
import com.rcp.exception.InvalidRoadTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeeCalculationServiceTest {

    private FeeCalculationService service;

    @BeforeEach
    void setUp() throws Exception {
        // Load the real rates.json from src/main/resources, exactly as the running application does -
        // these tests would fail if someone accidentally edited the shipped rate values.
        JsonRateConfigurationProvider provider =
                new JsonRateConfigurationProvider(new ClassPathResource("rates.json"), new ObjectMapper());
        provider.init();
        service = new FeeCalculationService(provider);
    }

    // ---- Required worked example A (spec section 13) ----
    @Test
    void workedExampleA_dehradun() {
        FeeBreakdown result = service.calculate(
                "dehradun", "BT",
                new BigDecimal("12.5"), new BigDecimal("1.2"),
                6, ApplicantType.PRIVATE,
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1)
        );

        assertThat(result.getAreaInSqm()).isEqualByComparingTo("15");
        assertThat(result.getRestorationCharge()).isEqualByComparingTo("18000");
        assertThat(result.getPermissionFee()).isEqualByComparingTo("1350");
        assertThat(result.getUrgencySurcharge()).isEqualByComparingTo("135");
        assertThat(result.getSecurityDeposit()).isEqualByComparingTo("5000");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("24485");
    }

    // ---- Required worked example B (spec section 14): haridwar overrides BT's day-rate and deposit floor ----
    @Test
    void workedExampleB_haridwarOverride() {
        FeeBreakdown result = service.calculate(
                "haridwar", "BT",
                new BigDecimal("12.5"), new BigDecimal("1.2"),
                6, ApplicantType.PRIVATE,
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1)
        );

        assertThat(result.getAreaInSqm()).isEqualByComparingTo("15");
        // restorationCharge is NOT overridden by haridwar - must fall back to the default rate (1200/sqm).
        assertThat(result.getRestorationCharge()).isEqualByComparingTo("18000");
        assertThat(result.getPermissionFee()).isEqualByComparingTo("1800");
        assertThat(result.getUrgencySurcharge()).isEqualByComparingTo("180");
        assertThat(result.getSecurityDeposit()).isEqualByComparingTo("7500");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("27480");
    }

    @Test
    void areaCeilingAppliedToProductNotToEachDimension() {
        // 12.5 * 1.2 = 15.0 exactly -> ceil(15.0) = 15 (already covered by example A/B, this test
        // uses dimensions that would give a DIFFERENT result if each dimension were ceil'd first:
        // ceil(2.1) * ceil(2.1) = 3 * 3 = 9, but ceil(2.1 * 2.1) = ceil(4.41) = 5.
        FeeBreakdown result = service.calculate(
                "dehradun", "BT",
                new BigDecimal("2.1"), new BigDecimal("2.1"),
                1, ApplicantType.PRIVATE,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1)
        );
        assertThat(result.getAreaInSqm()).isEqualByComparingTo("5");
    }

    @Test
    void governmentAgencyPaysNoPermissionFeeButStillPaysRestorationAndDeposit() {
        FeeBreakdown result = service.calculate(
                "dehradun", "BT",
                new BigDecimal("10"), new BigDecimal("10"),
                10, ApplicantType.GOVERNMENT_AGENCY,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1)
        );
        assertThat(result.getPermissionFee()).isEqualByComparingTo("0");
        // Permission fee is zero, so 10% of zero is also zero - surcharge must be zero too.
        assertThat(result.getUrgencySurcharge()).isEqualByComparingTo("0");
        assertThat(result.getRestorationCharge()).isEqualByComparingTo("120000"); // 100 sqm * 1200
        assertThat(result.getSecurityDeposit()).isEqualByComparingTo("30000");    // 25% of 120000 > min 5000
    }

    @Test
    void exactlyThreeDaysUntilStart_noSurcharge() {
        // Strict less-than: proposedStartDate exactly urgencyThresholdDays (3) away from applicationDate
        // must NOT attract a surcharge.
        FeeBreakdown result = service.calculate(
                "dehradun", "BT",
                new BigDecimal("10"), new BigDecimal("10"),
                5, ApplicantType.PRIVATE,
                LocalDate.of(2026, 1, 4), LocalDate.of(2026, 1, 1)
        );
        assertThat(result.getUrgencySurcharge()).isEqualByComparingTo("0");
    }

    @Test
    void twoDaysUntilStart_surchargeApplies() {
        FeeBreakdown result = service.calculate(
                "dehradun", "BT",
                new BigDecimal("10"), new BigDecimal("10"),
                5, ApplicantType.PRIVATE,
                LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 1)
        );
        // area=100, permissionFee = 100*15*5 = 7500, 10% surcharge = 750.
        assertThat(result.getUrgencySurcharge()).isEqualByComparingTo("750");
    }

    @Test
    void securityDepositUsesConfiguredFloorWhenPercentageIsLower() {
        // Small area -> the 25%-of-restoration figure is below the road type's minSecurityDeposit floor.
        FeeBreakdown result = service.calculate(
                "dehradun", "WBM",
                new BigDecimal("1"), new BigDecimal("1"),
                1, ApplicantType.PRIVATE,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1)
        );
        // restorationCharge = 1 * 650 = 650; 25% of 650 = 162.5 -> rounds to 163 (< min 2000).
        assertThat(result.getSecurityDeposit()).isEqualByComparingTo("2000");
    }

    @Test
    void inactiveRoadType_isRejected() {
        assertThatThrownBy(() -> service.calculate(
                "dehradun", "KUTCHA",
                BigDecimal.TEN, BigDecimal.TEN, 5, ApplicantType.PRIVATE,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(InvalidRoadTypeException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void unknownRoadType_isRejected() {
        assertThatThrownBy(() -> service.calculate(
                "dehradun", "GRAVEL",
                BigDecimal.TEN, BigDecimal.TEN, 5, ApplicantType.PRIVATE,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(InvalidRoadTypeException.class);
    }
}
