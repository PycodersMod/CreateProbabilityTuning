package com.pycoder.createprobabilitytuning.mixin;

import com.pycoder.createprobabilitytuning.nbt.ProcessData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;

/** Keeps CPT failure history when Create creates a new sequenced transitional item. */
@Mixin(SequencedAssemblyRecipe.class)
public abstract class SequencedAssemblyRecipeMixin {
    @Inject(method = "advance", at = @At("HEAD"), cancellable = true)
    private void cpt$handleFinalOutput(ResourceLocation recipeId, ItemStack input, RandomSource random,
                                       CallbackInfoReturnable<ItemStack> callback) {
        SequencedAssemblyRecipe recipe = (SequencedAssemblyRecipe) (Object) this;
        if (!input.has(com.simibubi.create.AllDataComponents.SEQUENCED_ASSEMBLY)) {
            return;
        }
        SequencedAssemblyRecipe.SequencedAssembly state =
                input.get(com.simibubi.create.AllDataComponents.SEQUENCED_ASSEMBLY);
        if (state == null || !state.id().equals(recipeId)
                || state.step() + 1 < recipe.getLoops() * recipe.getSequence().size()) {
            return;
        }
        com.pycoder.createprobabilitytuning.create.CreateHook hook =
                com.pycoder.createprobabilitytuning.create.CreateHook.active();
        if (hook != null) {
            hook.handleSequencedFinalOutput(recipeId, recipe, input, random)
                    .ifPresent(callback::setReturnValue);
        }
    }

    @Inject(method = "advance", at = @At("RETURN"))
    private void cpt$carryProcessData(ResourceLocation recipeId, ItemStack input, RandomSource random,
                                      CallbackInfoReturnable<ItemStack> callback) {
        ItemStack result = callback.getReturnValue();
        // The final failed sequence step returns the original ingredient, so it
        // no longer has Create's transitional SEQUENCED_ASSEMBLY component.
        // Carry our per-recipe history for every non-empty result, including
        // that final failure replacement.
        if (result != null && !result.isEmpty()) {
            ProcessData.copyModData(input, result);
            com.pycoder.createprobabilitytuning.create.SequenceOriginData.copy(input, result);
        }
    }
}
