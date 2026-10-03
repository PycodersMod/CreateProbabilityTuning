package com.pycoder.createprobabilitytuning.mixin;

import com.pycoder.createprobabilitytuning.create.CreateHook;
import com.pycoder.createprobabilitytuning.create.CreateOutputBridge;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.pycoder.createprobabilitytuning.recipe.ProcessingDecision;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** 覆盖 Create 的直接置物台/机械加工路径。 */
@Mixin(targets = "com.simibubi.create.foundation.recipe.RecipeApplier")
public abstract class RecipeApplierMixin {
    @Redirect(
            method = "applyRecipeOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/Recipe;Z)Ljava/util/List;",
            at = @At(value = "INVOKE", target =
                    "Lcom/simibubi/create/content/processing/recipe/ProcessingRecipe;rollResults(Ljava/util/List;Lnet/minecraft/util/RandomSource;)Ljava/util/List;"))
    private static List<ItemStack> cpt$applyProbability(ProcessingRecipe<?, ?> recipe,
                                                         List<ProcessingOutput> outputs,
                                                         RandomSource random,
                                                         Level level,
                                                         ItemStack input,
                                                         Recipe<?> originalRecipe,
                                                         boolean includeCraftingRemaining) {
        CreateHook hook = CreateHook.active();
        if (hook == null) {
            return recipe.rollResults(outputs, random);
        }
        if (input.has(AllDataComponents.SEQUENCED_ASSEMBLY)) {
            SequencedAssemblyRecipe.SequencedAssembly state =
                    input.get(AllDataComponents.SEQUENCED_ASSEMBLY);
            if (state != null && hook.configuredContexts().stream()
                    .filter(context -> context.recipeId().equals(state.id())
                            && context.recipe() instanceof SequencedAssemblyRecipe)
                    .map(context -> (SequencedAssemblyRecipe) context.recipe())
                    .anyMatch(parent -> parent.getLoops() * parent.getSequence().size() <= state.step() + 1)) {
                return hook.handleSequencedFinalOutput(state.id(), input, random, level.registryAccess())
                        .map(List::of)
                        .orElseGet(() -> recipe.rollResults(outputs, random));
            }
        }
        ProcessingDecision decision = hook.handleFinalProcessing(level, originalRecipe, input, outputs, random);
        if (decision.status() == ProcessingDecision.Status.DISABLED
                || decision.status() == ProcessingDecision.Status.UNAVAILABLE) {
            return recipe.rollResults(outputs, random);
        }
        return CreateOutputBridge.resolve(List.of(), decision.status(), decision.outputs());
    }
}
