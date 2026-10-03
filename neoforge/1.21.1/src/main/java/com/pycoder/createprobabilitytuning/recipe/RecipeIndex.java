package com.pycoder.createprobabilitytuning.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record RecipeIndex(Map<ResourceLocation, RecipeContext> byId) {
    public RecipeIndex {
        byId = Map.copyOf(byId);
    }

    public Optional<RecipeContext> find(ResourceLocation recipeId) {
        return Optional.ofNullable(byId.get(recipeId));
    }

    /** 查找由 RecipeManager/Create 提供的确切配方实例。 */
    public Optional<RecipeContext> findByRecipe(Recipe<?> recipe) {
        if (recipe instanceof RecipeOriginAccess access && access.cpt$getOrigin() != null) {
            return find(access.cpt$getOrigin());
        }
        return byId.values().stream()
                .filter(context -> context.recipe() == recipe)
                .findFirst();
    }
}
