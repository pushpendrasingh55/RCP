package com.rcp.dto.request;

import com.rcp.domain.ApplicantType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// The "Calculation" object as sent by the client to both _calculate and _create.
// Deliberately excludes any monetary field - the client can never submit a fee, only the inputs to compute one.
@Getter
@Setter
public class CalculationInput {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotBlank(message = "roadType is required")
    private String roadType;

    @NotNull(message = "lengthInMeters is required")
    @DecimalMin(value = "0.01", message = "lengthInMeters must be a positive number")
    private BigDecimal lengthInMeters;

    @NotNull(message = "widthInMeters is required")
    @DecimalMin(value = "0.01", message = "widthInMeters must be a positive number")
    private BigDecimal widthInMeters;

    @NotNull(message = "durationInDays is required")
    @Min(value = 1, message = "durationInDays must be at least 1")
    @Max(value = 365, message = "durationInDays must be a sane value (<= 365)")
    private Integer durationInDays;

    @NotNull(message = "applicantType is required")
    private ApplicantType applicantType;

    @NotNull(message = "proposedStartDate is required")
    private LocalDate proposedStartDate;
}
