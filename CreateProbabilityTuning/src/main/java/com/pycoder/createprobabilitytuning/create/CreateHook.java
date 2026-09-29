package com.pycoder.createprobabilitytuning.create;

import com.pycoder.createprobabilitytuning.recipe.ProcessingDecision;
import com.pycoder.createprobabilitytuning.recipe.RecipeContext;
import com.pycoder.createprobabilitytuning.recipe.RecipeIndex;
import com.pycoder.createprobabilitytuning.recipe.RecipeOverride;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.AllDataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.core.HolderLookup;

import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Version-sensitive Create adapter. Actual callback wiring is intentionally isolated here. */
public final class CreateHook {
    private static volatile CreateHook active;
    private final RecipeOverride override = new RecipeOverride();
    private volatile RecipeIndex index = new RecipeIndex(java.util.Map.of());
    private volatile RecipeIndex jeiIndex = new RecipeIndex(java.util.Map.of());
    private volatile ProcessingDecision lastDecision;
    private final Map<ProcessingOutput, Float> jeiChances = new IdentityHashMap<>();

    public CreateHook() {
        active = this;
    }

    public static CreateHook active() {
        return active;
    }

    public ProcessingDecision handleFinalProcessing(ResourceLocation recipeId, ItemStack input,
                                                     List<ProcessingOutput> outputs, RandomSource random) {
        return index.find(recipeId)
                .map(context -> evaluate(context, outputs, input, random))
                .orElseGet(() -> new ProcessingDecision(ProcessingDecision.Status.DISABLED, input, List.of()));
    }

    /** Handles Create's direct processing path, where only the recipe object is available. */
    public ProcessingDecision handleFinalProcessing(Recipe<?> recipe, ItemStack input,
                                                     List<ProcessingOutput> outputs, RandomSource random) {
        ProcessingDecision decision = contextForFinalProcessing(recipe, input)
            .map(context -> CreateSequenceSupport.restoreInput(context.recipe(), input,
                    evaluate(context, outputs, input, random)))
                .orElseGet(() -> new ProcessingDecision(ProcessingDecision.Status.DISABLED, input, List.of()));
        lastDecision = decision;
        return decision;
    }

    public ProcessingDecision handleFinalProcessing(Level level, Recipe<?> recipe, ItemStack input,
                                                     List<ProcessingOutput> outputs, RandomSource random) {
        if (CreateSequenceSupport.isConfiguredStep(index, recipe)
                && !input.has(AllDataComponents.SEQUENCED_ASSEMBLY)) {
            CreateSequenceSupport.captureOrigin(index, recipe, input, level.registryAccess());
        }
        ProcessingDecision decision = contextForFinalProcessing(recipe, input)
                .map(context -> CreateSequenceSupport.restoreInput(context.recipe(), input,
                        evaluate(context, outputs, input, random), context.recipeId(), level.registryAccess()))
                .orElseGet(() -> new ProcessingDecision(ProcessingDecision.Status.DISABLED, input, List.of()));
        lastDecision = decision;
        return decision;
    }

    /** Handles the result roll that Create performs internally after the final sequenced step. */
    public Optional<ItemStack> handleSequencedFinalOutput(ResourceLocation recipeId,
                                                          SequencedAssemblyRecipe recipe,
                                                          ItemStack input,
                                                          RandomSource random) {
        Optional<RecipeContext> context = index.find(recipeId);
        if (context.isEmpty()) {
            return Optional.empty();
        }
        ProcessingDecision decision = override.evaluateSequenced(
                recipeId, context.get().rule(), recipe.resultPool, input, random,
                recipe.getIngredient()::test);
        lastDecision = decision;
        if (decision.status() == ProcessingDecision.Status.SUCCESS) {
            return Optional.of(decision.outputs().isEmpty()
                    ? ItemStack.EMPTY
                    : decision.outputs().getFirst());
        }
        if (decision.status() == ProcessingDecision.Status.FAILURE && decision.returnsInput()) {
            return Optional.of(CreateSequenceSupport.restoreInput(recipe, input, decision).outputs().getFirst());
        }
        return Optional.of(ItemStack.EMPTY);
    }

    /**
     * Handles a final sequence step when Create reaches it through
     * RecipeApplier instead of invoking SequencedAssemblyRecipe.advance first.
     */
    public Optional<ItemStack> handleSequencedFinalOutput(ResourceLocation recipeId,
                                                           ItemStack input,
                                                           RandomSource random) {
        return handleSequencedFinalOutput(recipeId, input, random, null);
    }

    public Optional<ItemStack> handleSequencedFinalOutput(ResourceLocation recipeId,
                                                           ItemStack input,
                                                           RandomSource random,
                                                           HolderLookup.Provider registries) {
        return index.find(recipeId)
                .filter(context -> context.recipe() instanceof SequencedAssemblyRecipe)
                .map(context -> handleSequencedFinalOutput(recipeId,
                        (SequencedAssemblyRecipe) context.recipe(), input, random, registries))
                .orElseGet(Optional::empty);
    }

    private Optional<ItemStack> handleSequencedFinalOutput(ResourceLocation recipeId,
                                                           SequencedAssemblyRecipe recipe,
                                                           ItemStack input,
                                                           RandomSource random,
                                                           HolderLookup.Provider registries) {
        Optional<RecipeContext> context = index.find(recipeId);
        if (context.isEmpty()) return Optional.empty();
        ProcessingDecision decision = override.evaluateSequenced(recipeId, context.get().rule(),
                recipe.resultPool, input, random, recipe.getIngredient()::test);
        lastDecision = decision;
        if (decision.status() == ProcessingDecision.Status.SUCCESS) {
            return Optional.of(decision.outputs().isEmpty() ? ItemStack.EMPTY : decision.outputs().getFirst());
        }
        if (decision.status() == ProcessingDecision.Status.FAILURE && decision.returnsInput()) {
            ProcessingDecision restored = CreateSequenceSupport.restoreInput(recipe, input, decision,
                    recipeId, registries);
            return restored.outputs().isEmpty() ? Optional.of(ItemStack.EMPTY)
                    : Optional.of(restored.outputs().getFirst());
        }
        return Optional.of(ItemStack.EMPTY);
    }

    public void replaceIndex(RecipeIndex newIndex) {
        index = newIndex;
    }

    public void replaceJeiIndex(RecipeIndex newIndex) {
        jeiIndex = newIndex;
    }

    public List<RecipeContext> configuredContexts() {
        return List.copyOf(index.byId().values());
    }

    public List<RecipeContext> configuredJeiContexts() {
        return List.copyOf(jeiIndex.byId().values());
    }

    public synchronized void replaceJeiChances(Map<ProcessingOutput, Float> chances) {
        jeiChances.clear();
        jeiChances.putAll(chances);
    }

    public synchronized float configuredChance(ProcessingOutput output, float fallback) {
        return jeiChances.getOrDefault(output, fallback);
    }

    public float configuredChance(Recipe<?> recipe, float fallback) {
        return matchingJeiContext(recipe)
                .map(context -> {
                    if (context.recipe() instanceof SequencedAssemblyRecipe sequenced) {
                        return (float) override.calculateWeightedChance(context.rule(), sequenced.resultPool,
                                sequenced.getIngredient()::test, sequenced.resultPool.getFirst(), 0);
                    }
                    if (context.recipe() instanceof ProcessingRecipe<?, ?> processing
                            && !processing.getRollableResults().isEmpty()) {
                        ProcessingOutput main = processing.getRollableResults().getFirst();
                        return (float) override.calculateWeightedChance(context.rule(),
                                processing.getRollableResults(), ingredientFallback(processing), main, 0);
                    }
                    return fallback;
                })
                .orElse(fallback);
    }

    public float configuredChance(Recipe<?> recipe, ProcessingOutput output, float fallback) {
        return matchingJeiContext(recipe)
                .map(context -> {
                    if (context.recipe() instanceof ProcessingRecipe<?, ?> processing) {
                        return (float) override.calculateWeightedChance(context.rule(),
                                processing.getRollableResults(), ingredientFallback(processing), output, 0);
                    }
                    return fallback;
                })
                .orElse(fallback);
    }

    /**
     * Captures and evaluates the input before Create's real pass extracts it.
     * The simulated pass must never mutate the input or failure history.
     */
    public ProcessingDecision prepareBasinDecision(BasinBlockEntity basin, Recipe<?> recipe) {
        if (!(recipe instanceof ProcessingRecipe<?, ?> processingRecipe)) {
            return null;
        }
        for (ItemStack input : basinInputs(basin, processingRecipe)) {
            Optional<RecipeContext> context =
                    contextForFinalProcessing(recipe, input);
            if (context.isPresent()) {
                ProcessingDecision decision = CreateSequenceSupport.restoreInput(
                        context.get().recipe(), input,
                        evaluate(context.get(), processingRecipe.getRollableResults(), input,
                                basin.getLevel().random));
                lastDecision = decision;
                return decision;
            }
        }
        return null;
    }

    private Optional<RecipeContext> matchingContext(Recipe<?> recipe) {
        return index.findByRecipe(recipe);
    }

    private Optional<RecipeContext> matchingJeiContext(Recipe<?> recipe) {
        return jeiIndex.findByRecipe(recipe);
    }

    private Optional<RecipeContext> contextForFinalProcessing(
            Recipe<?> recipe, ItemStack input) {
        return CreateSequenceSupport.isConfiguredStep(index, recipe)
                ? Optional.empty() : matchingContext(recipe);
    }

    public ProcessingDecision lastDecision() {
        return lastDecision;
    }

    private ProcessingDecision evaluate(RecipeContext context, List<ProcessingOutput> outputs,
                                        ItemStack input, RandomSource random) {
        return override.evaluate(context.recipeId(), context.rule(), outputs, input, random,
                context.recipe() instanceof ProcessingRecipe<?, ?> processing
                        ? ingredientFallback(processing)
                        : stack -> stack.is(input.getItem()));
    }

    private static Predicate<ItemStack> ingredientFallback(ProcessingRecipe<?, ?> recipe) {
        return stack -> recipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(stack));
    }

    private static List<ItemStack> basinInputs(BasinBlockEntity basin, ProcessingRecipe<?, ?> recipe) {
        List<ItemStack> inputs = new java.util.ArrayList<>();
        for (int slot = 0; slot < basin.getInputInventory().getSlots(); slot++) {
            ItemStack stack = basin.getInputInventory().getStackInSlot(slot);
            if (!stack.isEmpty() && (recipe.getIngredients().isEmpty()
                    || recipe.getIngredients().getFirst().test(stack))) {
                inputs.add(stack);
            }
        }
        return inputs;
    }
}
