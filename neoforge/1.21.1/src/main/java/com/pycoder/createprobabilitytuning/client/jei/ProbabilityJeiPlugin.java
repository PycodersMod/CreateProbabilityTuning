package com.pycoder.createprobabilitytuning.client.jei;

import com.pycoder.createprobabilitytuning.CreateProbabilityTuning;
import com.pycoder.createprobabilitytuning.network.ClientRuleSync;
import com.pycoder.createprobabilitytuning.create.CreateHook;
import com.pycoder.createprobabilitytuning.recipe.RecipeContext;
import com.pycoder.createprobabilitytuning.recipe.RecipeScanner;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.IdentityHashMap;
import java.util.Map;

@JeiPlugin
public final class ProbabilityJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CreateProbabilityTuning.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        CreateHook hook = CreateHook.active();
        if (hook == null || Minecraft.getInstance().level == null) {
            return;
        }

        // The server recipe index is not available on the client used by JEI.
        // Build the same index from the synchronized client recipe manager first.
        hook.replaceJeiIndex(new RecipeScanner().scan(
                Minecraft.getInstance().level.getRecipeManager(),
                ClientRuleSync.config()));

        Map<ProcessingOutput, Float> chances = new IdentityHashMap<>();
        for (RecipeContext context : hook.configuredJeiContexts()) {
                if (context.recipe() instanceof ProcessingRecipe<?, ?> processing
                    && !processing.getRollableResults().isEmpty()) {
                for (ProcessingOutput output : processing.getRollableResults()) {
                    chances.put(output, hook.configuredChance(context.recipe(), output, output.getChance()));
                }
            }
        }
        hook.replaceJeiChances(chances);
    }
}
