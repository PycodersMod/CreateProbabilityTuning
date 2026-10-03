package com.pycoder.createprobabilitytuning.recipe;

import com.pycoder.createprobabilitytuning.config.RecipeRule;
import com.pycoder.createprobabilitytuning.nbt.ProcessData;
import com.pycoder.createprobabilitytuning.probability.ContinuousCalculator;
import com.pycoder.createprobabilitytuning.probability.DiscreteCalculator;
import com.pycoder.createprobabilitytuning.probability.ProbabilityCalculator;
import com.pycoder.createprobabilitytuning.probability.ProbabilityMath;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Create-specific boundary; the probability and NBT modules remain independent. */
public final class RecipeOverride {
    public ProcessingDecision evaluate(RecipeContext context, ItemStack input, RandomSource random) {
        if (!context.rule().enabled() || !(context.recipe() instanceof ProcessingRecipe<?, ?> processingRecipe)) {
            return new ProcessingDecision(ProcessingDecision.Status.DISABLED, input, List.of());
        }
        List<ProcessingOutput> outputs = processingRecipe.getRollableResults();
        Predicate<ItemStack> fallback = stack -> processingRecipe.getIngredients().stream()
                .anyMatch(ingredient -> ingredient.test(stack));
        return evaluate(context.recipeId(), context.rule(), outputs, input, random, fallback);
    }

    public ProcessingDecision evaluate(RecipeRule rule, List<ProcessingOutput> outputs, ItemStack input, RandomSource random) {
        return evaluate(null, rule, outputs, input, random);
    }

    public ProcessingDecision evaluate(ResourceLocation recipeId, RecipeRule rule, List<ProcessingOutput> outputs,
                                       ItemStack input, RandomSource random) {
        return evaluate(recipeId, rule, outputs, input, random,
                stack -> stack.is(input.getItem()));
    }

    public ProcessingDecision evaluate(ResourceLocation recipeId, RecipeRule rule, List<ProcessingOutput> outputs,
                                       ItemStack input, RandomSource random,
                                       Predicate<ItemStack> originalFallback) {
        if (!rule.enabled() || outputs.isEmpty()) {
            return new ProcessingDecision(ProcessingDecision.Status.DISABLED, input, List.of());
        }
        ItemStack snapshot = input.copyWithCount(1);
        int failures = recipeId == null ? ProcessData.readAttempts(snapshot) : ProcessData.readAttempts(snapshot, recipeId);
        final double chance;
        try {
            chance = configuredChance(rule, outputs, originalFallback, failures);
        } catch (RuntimeException exception) {
            return new ProcessingDecision(ProcessingDecision.Status.UNAVAILABLE, input, List.of(), false);
        }
        ProcessingOutput selected = random.nextDouble() < chance
                ? selectEligibleOutput(outputs, originalFallback, random)
                : null;
        if (selected != null) {
            if (recipeId == null) {
                ProcessData.clear(snapshot);
            } else {
                ProcessData.clear(snapshot, recipeId);
            }
            return new ProcessingDecision(ProcessingDecision.Status.SUCCESS, snapshot,
                    List.of(selected.getStack().copy()));
        }
        if (recipeId == null) {
            ProcessData.withFailure(snapshot);
        } else {
            ProcessData.withFailure(snapshot, recipeId);
        }
        int currentAttempts = recipeId == null ? ProcessData.readAttempts(snapshot) : ProcessData.readAttempts(snapshot, recipeId);
        if (rule.maxN() > 0 && (long) currentAttempts >= rule.maxN()) {
            return new ProcessingDecision(ProcessingDecision.Status.FAILURE, snapshot, List.of(), false);
        }
        return new ProcessingDecision(ProcessingDecision.Status.FAILURE, snapshot,
                List.of(snapshot.copy()), true);
    }

    /**
     * Create's sequenced result pool is an exclusive weighted pool, unlike the
     * independent main/extra outputs used by ordinary processing recipes.
     */
    public ProcessingDecision evaluateSequenced(ResourceLocation recipeId, RecipeRule rule,
                                                 List<ProcessingOutput> resultPool, ItemStack input,
                                                 RandomSource random) {
        return evaluateSequenced(recipeId, rule, resultPool, input, random,
                stack -> stack.is(input.getItem()));
    }

    public ProcessingDecision evaluateSequenced(ResourceLocation recipeId, RecipeRule rule,
                                                 List<ProcessingOutput> resultPool, ItemStack input,
                                                 RandomSource random, Predicate<ItemStack> originalFallback) {
        if (!rule.enabled() || resultPool.isEmpty()) {
            return new ProcessingDecision(ProcessingDecision.Status.DISABLED, input, List.of());
        }
        int failures = ProcessData.readAttempts(input, recipeId);
        return evaluate(recipeId, rule, resultPool, input, random, originalFallback);
    }

    public double calculateWeightedChance(RecipeRule rule, List<ProcessingOutput> outputs,
                                          Predicate<ItemStack> originalFallback,
                                          ProcessingOutput target, int attempts) {
        double eligibleWeight = eligibleWeight(outputs, originalFallback);
        if (eligibleWeight <= 0.0 || originalFallback.test(target.getStack())) {
            return 0.0;
        }
        return ProbabilityMath.clamp(calculateMainChance(rule, attempts), 0.0, 1.0)
                * Math.max(0.0f, target.getChance()) / eligibleWeight;
    }

    public double calculateWeightedFallbackChance(RecipeRule rule, List<ProcessingOutput> outputs,
                                                  Predicate<ItemStack> originalFallback, int attempts) {
        return 1.0 - ProbabilityMath.clamp(calculateMainChance(rule, attempts), 0.0, 1.0);
    }

    private double configuredChance(RecipeRule rule, List<ProcessingOutput> outputs,
                                    Predicate<ItemStack> originalFallback, int attempts) {
        double eligibleWeight = eligibleWeight(outputs, originalFallback);
        if (eligibleWeight <= 0.0) {
            return 0.0;
        }
        return ProbabilityMath.clamp(calculateMainChance(rule, attempts), 0.0, 1.0);
    }

    private ProcessingOutput selectEligibleOutput(List<ProcessingOutput> outputs,
                                                  Predicate<ItemStack> originalFallback,
                                                  RandomSource random) {
        double total = eligibleWeight(outputs, originalFallback);
        if (total <= 0.0) {
            return null;
        }
        double roll = random.nextDouble() * total;
        ProcessingOutput last = null;
        for (ProcessingOutput output : outputs) {
            double weight = Math.max(0.0f, output.getChance());
            if (weight <= 0.0 || originalFallback.test(output.getStack())) {
                continue;
            }
            last = output;
            roll -= weight;
            if (roll < 0.0) {
                return output;
            }
        }
        return last;
    }

    private double eligibleWeight(List<ProcessingOutput> outputs, Predicate<ItemStack> originalFallback) {
        return outputs.stream()
                .filter(output -> !originalFallback.test(output.getStack()))
                .mapToDouble(output -> Math.max(0.0f, output.getChance()))
                .sum();
    }

    public double calculateMainChance(RecipeRule rule, int attempts) {
        ProbabilityCalculator calculator = rule.mode() == RecipeRule.Mode.DISCRETE
                ? new DiscreteCalculator(rule.changes())
                : new ContinuousCalculator(rule.formula());
        return calculator.calculate(rule.initial(), attempts);
    }
}
