package com.pycoder.createprobabilitytuning.probability;

import java.util.List;

public final class DiscreteCalculator implements ProbabilityCalculator {
    private final List<Double> changes;

    public DiscreteCalculator(List<Double> changes) {
        if (changes == null || changes.isEmpty()) {
            throw new IllegalArgumentException("离散概率变化列表不能为空");
        }
        this.changes = List.copyOf(changes);
    }

    @Override
    public double calculate(double initial, int failures) {
        double probability = initial;
        int count = Math.max(0, failures);
        for (int index = 0; index < count; index++) {
            probability += changes.get(Math.min(index, changes.size() - 1));
        }
        return probability;
    }
}
