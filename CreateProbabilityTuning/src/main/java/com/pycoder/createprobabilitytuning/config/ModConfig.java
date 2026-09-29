package com.pycoder.createprobabilitytuning.config;

import java.util.List;
import java.util.Map;

public record ModConfig(Map<String, RecipeRule> recipes, List<ConfigDiagnostic> diagnostics) {
    public ModConfig {
        recipes = Map.copyOf(recipes);
        diagnostics = List.copyOf(diagnostics);
    }

    public static ModConfig emptyWith(ConfigDiagnostic diagnostic) {
        return new ModConfig(Map.of(), List.of(diagnostic));
    }
}
