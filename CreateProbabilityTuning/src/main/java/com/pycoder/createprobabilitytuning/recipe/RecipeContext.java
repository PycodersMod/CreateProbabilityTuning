package com.pycoder.createprobabilitytuning.recipe;

import com.pycoder.createprobabilitytuning.config.RecipeRule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

public record RecipeContext(ResourceLocation recipeId, Recipe<?> recipe, RecipeRule rule) {
}
