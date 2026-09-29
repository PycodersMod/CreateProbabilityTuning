package com.pycoder.createprobabilitytuning.create;

import com.pycoder.createprobabilitytuning.nbt.ProcessData;
import com.pycoder.createprobabilitytuning.recipe.ProcessingDecision;
import com.pycoder.createprobabilitytuning.recipe.RecipeIndex;
import com.pycoder.createprobabilitytuning.recipe.RecipeContext;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Shared rules for Create's transitional sequenced-assembly items. */
final class CreateSequenceSupport {
    private CreateSequenceSupport() {
    }

    static boolean isConfiguredStep(RecipeIndex index, Recipe<?> recipe) {
        return findParent(index, recipe).isPresent();
    }

    static void captureOrigin(RecipeIndex index, Recipe<?> recipe, ItemStack input,
                              HolderLookup.Provider registries) {
        findParent(index, recipe).ifPresent(parent ->
                SequenceOriginData.capture(input, parent.recipeId(), input, registries));
    }

    private static java.util.Optional<RecipeContext> findParent(RecipeIndex index, Recipe<?> recipe) {
        return index.byId().values().stream()
                .filter(context -> context.recipe() instanceof SequencedAssemblyRecipe)
                .filter(context -> ((SequencedAssemblyRecipe) context.recipe()).getSequence().stream()
                        .map(SequencedRecipe::getRecipe)
                        .anyMatch(step -> step == recipe))
                .findFirst();
    }

    static ProcessingDecision restoreInput(Recipe<?> recipe, ItemStack input,
                                           ProcessingDecision decision) {
        return restoreInput(recipe, input, decision, null, null);
    }

    static ProcessingDecision restoreInput(Recipe<?> recipe, ItemStack input,
                                           ProcessingDecision decision,
                                           ResourceLocation recipeId,
                                           HolderLookup.Provider registries) {
        if (decision.status() != ProcessingDecision.Status.FAILURE
                || !decision.returnsInput()
                || decision.outputs().isEmpty()
                || !(recipe instanceof SequencedAssemblyRecipe parent)
                || !input.has(AllDataComponents.SEQUENCED_ASSEMBLY)
                || parent.getIngredient().getItems().length == 0) {
            return decision;
        }
        ItemStack failedOutput = decision.outputs().getFirst();
        ItemStack initial = registries != null && recipeId != null
                ? SequenceOriginData.read(input, recipeId, registries)
                    .map(stack -> stack.copyWithCount(failedOutput.getCount()))
                    .orElseGet(() -> parent.getIngredient().getItems()[0].copyWithCount(failedOutput.getCount()))
                : parent.getIngredient().getItems()[0].copyWithCount(failedOutput.getCount());
        ProcessData.copyModData(failedOutput, initial);
        if (registries != null && recipeId != null) {
            SequenceOriginData.clear(initial);
        }
        List<ItemStack> outputs = new ArrayList<>(decision.outputs());
        outputs.set(0, initial);
        return new ProcessingDecision(decision.status(), decision.input(), outputs, decision.returnsInput());
    }
}
