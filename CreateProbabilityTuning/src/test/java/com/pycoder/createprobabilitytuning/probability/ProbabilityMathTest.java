package com.pycoder.createprobabilitytuning.probability;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProbabilityMathTest {
    @Test
    void clampsOnlyAtTheBoundary() {
        assertEquals(0.0, ProbabilityMath.clamp(-2.0, 0.0, 1.0));
        assertEquals(1.0, ProbabilityMath.clamp(2.0, 0.0, 1.0));
        assertEquals(0.5, ProbabilityMath.clamp(0.5, 0.0, 1.0));
        assertTrue(Double.isNaN(ProbabilityMath.clamp(Double.NaN, 0.0, 1.0)));
    }

    @Test
    void handlesInfiniteValuesAndNanInRandomRoll() {
        assertEquals(1.0, ProbabilityMath.clamp(Double.POSITIVE_INFINITY, 0.0, 1.0));
        assertEquals(0.0, ProbabilityMath.clamp(Double.NEGATIVE_INFINITY, 0.0, 1.0));
        assertFalse(ProbabilityMath.roll(new Random(1), Double.NaN));
        assertTrue(ProbabilityMath.roll(new Random(1), Double.POSITIVE_INFINITY));
    }
}
