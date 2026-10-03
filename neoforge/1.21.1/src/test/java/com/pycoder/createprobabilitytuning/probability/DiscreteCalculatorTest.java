package com.pycoder.createprobabilitytuning.probability;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscreteCalculatorTest {
    private final DiscreteCalculator calculator = new DiscreteCalculator(List.of(0.2, 0.1, 0.05, 0.0));

    @Test
    void returnsInitialProbabilityBeforeAnyFailure() {
        assertEquals(0.2, calculator.calculate(0.2, 0), 1.0e-9);
    }

    @Test
    void accumulatesChangesForEachFailure() {
        assertEquals(0.4, calculator.calculate(0.2, 1), 1.0e-9);
        assertEquals(0.5, calculator.calculate(0.2, 2), 1.0e-9);
    }

    @Test
    void repeatsTheLastChangeAfterTheListEnds() {
        assertEquals(0.55, calculator.calculate(0.2, 5), 1.0e-9);
    }
}
