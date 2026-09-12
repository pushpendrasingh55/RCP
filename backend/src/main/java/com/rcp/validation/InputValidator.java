package com.rcp.validation;

import com.rcp.dto.request.CalculationInput;
import com.rcp.exception.ValidationFailedException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

// Business-rule validation that Bean Validation annotations alone can't express, because it depends
// on "now" (today's date) rather than just the shape of the payload.
@Component
public class InputValidator {

    public void assertProposedStartDateNotInPast(CalculationInput input, LocalDate today) {
        if (input.getProposedStartDate().isBefore(today)) {
            throw new ValidationFailedException(
                    "proposedStartDate " + input.getProposedStartDate() + " must not be in the past");
        }
    }
}
