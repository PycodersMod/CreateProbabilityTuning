package com.pycoder.createprobabilitytuning.mixin;

import com.pycoder.createprobabilitytuning.create.CreateHook;
import com.simibubi.create.compat.jei.category.SequencedAssemblyCategory;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Applies configured probability to Create's existing sequenced-assembly JEI page. */
@Mixin(SequencedAssemblyCategory.class)
public abstract class SequencedAssemblyCategoryMixin {
    @Redirect(
            method = {"setRecipe", "draw", "getTooltipStrings", "lambda$setRecipe$0"},
            at = @At(value = "INVOKE", target =
                    "Lcom/simibubi/create/content/processing/sequenced/SequencedAssemblyRecipe;getOutputChance()F"))
    private float cpt$configuredOutputChance(SequencedAssemblyRecipe recipe) {
        CreateHook hook = CreateHook.active();
        return hook == null ? recipe.getOutputChance() : hook.configuredChance(recipe, recipe.getOutputChance());
    }
}
