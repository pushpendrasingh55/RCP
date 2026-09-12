package com.rcp.calculation;

import com.rcp.domain.ApplicantType;
import com.rcp.exception.InvalidRoadTypeException;
import com.rcp.util.MoneyUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// Implements section 3 of the spec, rule by rule. This class depends only on RateConfigurationProvider
// (the abstraction), never on JsonRateConfigurationProvider or the rates.json file directly.
@Service
public class FeeCalculationService {

    private final RateConfigurationProvider rateConfigurationProvider;

    public FeeCalculationService(RateConfigurationProvider rateConfigurationProvider) {
        this.rateConfigurationProvider = rateConfigurationProvider;
    }

    public FeeBreakdown calculate(String tenantId,
                                   String roadType,
                                   BigDecimal lengthInMeters,
                                   BigDecimal widthInMeters,
                                   int durationInDays,
                                   ApplicantType applicantType,
                                   LocalDate proposedStartDate,
                                   LocalDate applicationDate) {

        // Look up the tenant-resolved rate for this road type, and reject unknown/inactive codes up front -
        // every rule below assumes a valid, active rate exists.
        RoadTypeRate rate = rateConfigurationProvider.getRoadTypeRate(tenantId, roadType)
                .orElseThrow(() -> new InvalidRoadTypeException(
                        "Road type " + roadType + " is not recognised for tenant " + tenantId));
        if (!rate.isActive()) {
            throw new InvalidRoadTypeException(
                    "Road type " + roadType + " is not active for tenant " + tenantId);
        }

        GlobalRateSettings settings = rateConfigurationProvider.getGlobalSettings(tenantId);

        // Rule 1: area = ceil(length * width) - ceiling is applied to the PRODUCT, not to each dimension.
        // We multiply at full precision, then round the single product up to a whole square metre.
        BigDecimal area = lengthInMeters.multiply(widthInMeters).setScale(0, RoundingMode.CEILING);

        // Rule 2: restorationCharge = area * restorationRatePerSqm. Both factors are whole numbers by
        // construction (area is ceil'd; rate is a whole-rupee figure from config), so this is exact - no rounding needed.
        BigDecimal restorationCharge = area.multiply(rate.getRestorationRatePerSqm());

        // Rule 3: permissionFee = area * permissionRatePerSqmPerDay * durationInDays.
        BigDecimal permissionFee = area
                .multiply(rate.getPermissionRatePerSqmPerDay())
                .multiply(BigDecimal.valueOf(durationInDays));

        // Rule 4: government agencies pay no permission fee (restoration + deposit still apply).
        // This must happen before rule 5, since the urgency surcharge is a percentage OF permissionFee -
        // zeroing it here naturally zeroes the surcharge too, without a separate special case.
        if (applicantType == ApplicantType.GOVERNMENT_AGENCY) {
            permissionFee = BigDecimal.ZERO;
        }

        // Rule 5: urgency surcharge applies only when the proposed start date is STRICTLY less than
        // urgencyThresholdDays away from the application date. Exactly on the threshold => no surcharge.
        long daysUntilStart = ChronoUnit.DAYS.between(applicationDate, proposedStartDate);
        BigDecimal urgencySurcharge;
        if (daysUntilStart < settings.getUrgencyThresholdDays()) {
            urgencySurcharge = MoneyUtil.percentOfRoundedToWhole(permissionFee, settings.getUrgencySurchargePercent());
        } else {
            urgencySurcharge = BigDecimal.ZERO;
        }

        // Rule 6: securityDeposit = max(minSecurityDeposit, securityDepositPercent% of restorationCharge).
        BigDecimal percentageOfRestoration =
                MoneyUtil.percentOfRoundedToWhole(restorationCharge, settings.getSecurityDepositPercent());
        BigDecimal securityDeposit = rate.getMinSecurityDeposit().max(percentageOfRestoration);

        // Rule 7: total = round_half_up(sum of all four components). Every component above is already
        // a whole-currency-unit BigDecimal, so this rounding is a formality that guards against any
        // future change to the rules above introducing fractional intermediate values.
        BigDecimal total = MoneyUtil.roundHalfUpToWhole(
                restorationCharge.add(permissionFee).add(urgencySurcharge).add(securityDeposit));

        return new FeeBreakdown(area, restorationCharge, permissionFee, urgencySurcharge, securityDeposit, total);
    }
}
