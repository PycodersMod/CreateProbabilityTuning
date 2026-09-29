package com.pycoder.createprobabilitytuning.probability;

import java.util.random.RandomGenerator;

public final class ProbabilityMath {
    private ProbabilityMath() {
    }

    public static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return Double.NaN;
        }
        return Math.max(min, Math.min(max, value));
    }

    public static boolean roll(RandomGenerator random, double probability) {
        if (Double.isNaN(probability)) {
            return false;
        }
        return random.nextDouble() < clamp(probability, 0.0, 1.0);
    }
}
