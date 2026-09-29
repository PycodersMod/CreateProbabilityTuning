package com.pycoder.createprobabilitytuning.recipe;

import com.pycoder.createprobabilitytuning.config.RecipeRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeOverrideTest {
    private final RecipeOverride override = new RecipeOverride();



    @Test
    void discreteChanceUsesFailureHistory() {
        RecipeRule rule = new RecipeRule(true, 0.1, RecipeRule.Mode.DISCRETE, List.of(0.2, 0.3), null);
        assertEquals(0.6, override.calculateMainChance(rule, 2), 1.0e-12);
    }

    @Test
    void continuousChanceSumsFromOneThroughFailures() {
        RecipeRule rule = new RecipeRule(true, 0.1, RecipeRule.Mode.CONTINUOUS, List.of(), "0.05*n");
        assertEquals(0.4, override.calculateMainChance(rule, 3), 1.0e-12);
    }

}
