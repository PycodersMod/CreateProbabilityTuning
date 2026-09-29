package com.pycoder.createprobabilitytuning.probability;

@FunctionalInterface
public interface ProbabilityCalculator {
    double calculate(double initial, int failures);
}
