package com.pycoder.createprobabilitytuning.recipe;

import com.pycoder.createprobabilitytuning.config.RecipeRule;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeIndexTest {
    @Test
    void findsConfiguredRecipeAndKeepsIndexImmutable() {
        ResourceLocation id = ResourceLocation.parse("create:test");
        RecipeContext context = new RecipeContext(id, null,
                new RecipeRule(true, 0.5, RecipeRule.Mode.DISCRETE, java.util.List.of(), null));
        RecipeIndex index = new RecipeIndex(Map.of(id, context));

        assertTrue(index.find(id).isPresent());
        assertEquals(context, index.find(id).orElseThrow());
        assertTrue(index.byId().containsKey(id));
    }
}
