package com.pycoder.createprobabilitytuning.recipe;

import com.mojang.logging.LogUtils;
import com.pycoder.createprobabilitytuning.config.ConfigDiagnostic;
import com.pycoder.createprobabilitytuning.config.ModConfig;
import com.pycoder.createprobabilitytuning.config.RecipeRule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecipeScanner {
    private static final Logger LOGGER = LogUtils.getLogger();

    public RecipeIndex scan(RecipeManager recipeManager, ModConfig config) {
        Map<ResourceLocation, RecipeContext> index = new HashMap<>();
        for (Map.Entry<String, RecipeRule> entry : config.recipes().entrySet()) {
            RecipeRule rule = entry.getValue();
            if (!rule.enabled()) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null) {
                LOGGER.warn("Skipping invalid recipe ID {}", entry.getKey());
                continue;
            }
            recipeManager.byKey(id).ifPresentOrElse(
                    holder -> {
                        RecipeContext context = new RecipeContext(id, holder.value(), rule);
                        index.put(id, context);
                        // Create derives deployer recipes from manual application
                        // recipes. Keep the configured manual recipe id as the
                        // history key while indexing the derived recipe object.
                          if (holder.value() instanceof ManualApplicationRecipe) {
                              ResourceLocation deployerId = id.withSuffix("_using_deployer");
                              recipeManager.byKey(deployerId).ifPresent(deployer ->
                                      index.put(deployerId, new RecipeContext(id, deployer.value(), rule)));
                          }
                    },
                    () -> LOGGER.warn("Configured recipe {} was not found", id));
        }
        return new RecipeIndex(index);
    }

    public static List<ConfigDiagnostic> missingDiagnostics(RecipeManager recipeManager, ModConfig config) {
        return config.recipes().keySet().stream()
                .filter(id -> config.recipes().get(id).enabled())
                .map(ResourceLocation::tryParse)
                  .filter(id -> id != null && recipeManager.byKey(id).isEmpty()
                         && !isManualApplication(recipeManager, id))
                .map(id -> ConfigDiagnostic.warning(id.toString(), "Configured recipe was not found"))
                .toList();
    }

    private static boolean isManualApplication(RecipeManager recipeManager, ResourceLocation id) {
        return recipeManager.byKey(id)
                .map(holder -> holder.value() instanceof ManualApplicationRecipe
                        && recipeManager.byKey(id.withSuffix("_using_deployer")).isPresent())
                .orElse(false);
    }
}
