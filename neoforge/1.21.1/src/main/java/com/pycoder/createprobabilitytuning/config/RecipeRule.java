package com.pycoder.createprobabilitytuning.config;

import java.util.List;

public record RecipeRule(boolean enabled, double initial, Mode mode, List<Double> changes, String formula, int maxN) {
    public RecipeRule(boolean enabled, double initial, Mode mode, List<Double> changes, String formula) {
        this(enabled, initial, mode, changes, formula, 0);
    }

    public RecipeRule {
        changes = changes == null ? List.of() : List.copyOf(changes);
        formula = formula == null ? "" : formula;
        if (maxN < 0) {
            throw new IllegalArgumentException("maxN must be non-negative");
        }
    }

    public enum Mode {
        DISCRETE,
        CONTINUOUS
    }
}
