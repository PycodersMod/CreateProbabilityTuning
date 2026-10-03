package com.pycoder.createprobabilitytuning.probability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaValidatorTest {
    private final FormulaValidator validator = new FormulaValidator();

    @Test
    void acceptsFiniteFormula() {
        FormulaValidator.Result result = validator.validate("0.05*sin(n)");

        assertTrue(result.valid());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void rejectsUnknownVariable() {
        FormulaValidator.Result result = validator.validate("0.05*m");

        assertFalse(result.valid());
        assertTrue(result.hasError());
    }

    @Test
    void rejectsNonFiniteFormula() {
        FormulaValidator.Result result = validator.validate("1/(n-n)");

        assertFalse(result.valid());
        assertTrue(result.hasError());
    }

    @Test
    void warnsAboutExtremeGrowth() {
        FormulaValidator.Result result = validator.validate("n^4.1");

        assertTrue(result.valid());
        assertTrue(result.hasWarning());
    }

    @Test
    void finiteMaxChecksOnlyReachableFailureIncrements() {
        FormulaValidator.Result result = validator.validate("1/(n-1)", 1);

        assertTrue(result.valid());
    }

    @Test
    void unboundedExtremeFormulaRemainsUsableWithWarning() {
        FormulaValidator.Result result = validator.validate("exp(n)/150", 0);

        assertTrue(result.valid());
        assertTrue(result.hasWarning());
    }
}
