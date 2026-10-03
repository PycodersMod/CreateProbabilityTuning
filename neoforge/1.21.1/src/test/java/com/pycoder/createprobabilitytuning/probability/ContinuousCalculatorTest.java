package com.pycoder.createprobabilitytuning.probability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContinuousCalculatorTest {
    @Test
    void sumsFormulaValuesFromOneThroughFailureCount() {
        ContinuousCalculator calculator = new ContinuousCalculator("0.05*n");

        assertEquals(0.2, calculator.calculate(0.2, 0), 1.0e-9);
        assertEquals(0.35, calculator.calculate(0.2, 2), 1.0e-9);
    }

    @Test
    void supportsTrigonometricFormula() {
        ContinuousCalculator calculator = new ContinuousCalculator("0.05*sin(n)");

        double expected = 0.2 + 0.05 * Math.sin(1.0) + 0.05 * Math.sin(2.0);
        assertEquals(expected, calculator.calculate(0.2, 2), 1.0e-9);
    }

    @Test
    void calculatesExponentialFormulaUsingN() {
        ContinuousCalculator calculator = new ContinuousCalculator("exp(n)/150");

        assertEquals(0.1 + Math.exp(1) / 150.0 + Math.exp(2) / 150.0,
                calculator.calculate(0.1, 2), 1.0e-12);
    }

    @Test
    void followsMathematicalExponentPrecedence() {
        ContinuousCalculator calculator = new ContinuousCalculator("-n^2");

        assertEquals(-5.0, calculator.calculate(0.0, 2), 1.0e-12);
    }

    @Test
    void rejectsUnexpectedFunctionArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> new ContinuousCalculator("sin(n, 2)"));
    }

    @Test
    void rejectsUnboundedRuntimeWorkBeforeEnteringTheLoop() {
        ContinuousCalculator calculator = new ContinuousCalculator("0.01");

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(0.0, 4097));
    }
}
