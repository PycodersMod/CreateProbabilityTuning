package com.pycoder.createprobabilitytuning.gametest;

import com.pycoder.createprobabilitytuning.CreateProbabilityTuning;
import com.pycoder.createprobabilitytuning.create.CreateHook;
import com.pycoder.createprobabilitytuning.config.RecipeRule;
import com.pycoder.createprobabilitytuning.config.ModConfig;
import com.pycoder.createprobabilitytuning.recipe.RecipeContext;
import com.pycoder.createprobabilitytuning.recipe.RecipeIndex;
import com.pycoder.createprobabilitytuning.recipe.RecipeScanner;
import com.pycoder.createprobabilitytuning.recipe.RecipeOverride;
import com.pycoder.createprobabilitytuning.nbt.ProcessData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.item.Items;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;

import java.util.List;
import java.util.Map;

@GameTestHolder(CreateProbabilityTuning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProbabilityTuningGameTests {
    private ProbabilityTuningGameTests() {
    }

    @GameTest(template = "gametest/processing/brass_mixing", templateNamespace = CreateProbabilityTuning.MOD_ID)
    public static void processDataFailureHistory(GameTestHelper helper) {
        try {
            Class<?> basinRecipe = Class.forName("com.simibubi.create.content.processing.basin.BasinRecipe");
            boolean mixinApplied = java.util.Arrays.stream(basinRecipe.getDeclaredMethods())
                    .anyMatch(method -> method.getName().contains("cpt$acceptOutputs"));
            if (!mixinApplied) {
                helper.fail("BasinRecipe Mixin bridge method was not applied");
                return;
            }
        } catch (Throwable error) {
            helper.fail("BasinRecipe Mixin target failed to load: " + error);
            return;
        }
        CompoundTag root = new CompoundTag();
        ProcessData.withFailure(root);
        ProcessData.withFailure(root);
        if (ProcessData.readAttempts(root) != 2) {
            helper.fail("Expected two recorded failures");
            return;
        }
        ProcessData.clear(root);
        if (ProcessData.hasAttempts(root)) {
            helper.fail("Expected successful processing to clear attempts");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/processing/brass_mixing", templateNamespace = CreateProbabilityTuning.MOD_ID)
    public static void manualApplicationProbabilityMixinIsLoaded(GameTestHelper helper) {
        try {
            Class<?> recipeClass = Class.forName(
                    "com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe");
            boolean loaded = java.util.Arrays.stream(recipeClass.getDeclaredMethods())
                    .anyMatch(method -> method.getName().contains("cpt$manualApplication"));
            if (!loaded) {
                helper.fail("ManualApplicationRecipe probability bridge was not applied");
                return;
            }
            helper.succeed();
        } catch (Throwable error) {
            helper.fail("ManualApplicationRecipe Mixin target failed to load: " + error);
        }
    }

    @GameTest(template = "gametest/processing/brass_mixing", templateNamespace = CreateProbabilityTuning.MOD_ID)
    public static void manualApplicationFailureReturnsTaggedBlockItem(GameTestHelper helper) {
        try {
            var recipeId = net.minecraft.resources.ResourceLocation.parse(
                    "create:item_application/andesite_casing_from_log");
            var recipe = helper.getLevel().getRecipeManager().byKey(recipeId)
                    .map(holder -> holder.value())
                    .filter(ManualApplicationRecipe.class::isInstance)
                    .map(ManualApplicationRecipe.class::cast)
                    .orElseThrow(() -> new IllegalStateException("Andesite casing manual recipe not found"));
            CreateHook.active().replaceIndex(new RecipeIndex(Map.of(recipeId,
                    new RecipeContext(recipeId, recipe,
                            new RecipeRule(true, 0.0, RecipeRule.Mode.CONTINUOUS, List.of(), "exp(n)/150")))));
            var pos = helper.absolutePos(new BlockPos(2, 1, 2));
            helper.getLevel().setBlock(pos, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), 3);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack applicationItem = recipe.getIngredients().get(1).getItems()[0].copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, applicationItem);
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var event = new PlayerInteractEvent.RightClickBlock(
                    player, InteractionHand.MAIN_HAND, pos, hit);
            ManualApplicationRecipe.manualApplicationRecipesApplyInWorld(event);

            if (!helper.getLevel().getBlockState(pos).is(Blocks.STRIPPED_OAK_LOG)) {
                helper.fail("A failed manual application must keep the source block in place");
                return;
            }
            ItemStack stored = com.pycoder.createprobabilitytuning.nbt.PlacedProcessData.get(
                    (net.minecraft.server.level.ServerLevel) helper.getLevel())
                    .get(pos, Blocks.STRIPPED_OAK_LOG.asItem());
            if (stored.isEmpty() || ProcessData.readAttempts(stored, recipeId) != 1) {
                helper.fail("Expected the placed stripped log to retain Attempts=1, stored=" + stored);
                return;
            }
            helper.succeed();
        } catch (Throwable error) {
            helper.fail("Manual application behavior test failed: " + error);
        }
    }

    @GameTest(template = "gametest/processing/brass_mixing", templateNamespace = CreateProbabilityTuning.MOD_ID)
    public static void recipeIndexLifecycle(GameTestHelper helper) {
        var recipeManager = helper.getLevel().getRecipeManager();
        var existing = "create:mixing/brass_ingot";
        var missing = "create:mixing/definitely_missing_for_game_test";
        var rule = new RecipeRule(true, 0.5, RecipeRule.Mode.DISCRETE, List.of(0.0), "");
        var scanner = new RecipeScanner();
        var first = scanner.scan(recipeManager, new ModConfig(Map.of(existing, rule, missing, rule), List.of()));
        var existingId = net.minecraft.resources.ResourceLocation.parse(existing);
        var missingId = net.minecraft.resources.ResourceLocation.parse(missing);
        if (first.find(existingId).isEmpty() || first.find(missingId).isPresent()) {
            helper.fail("Recipe scan did not separate existing and missing Recipe IDs");
            return;
        }
        var second = scanner.scan(recipeManager, new ModConfig(Map.of(missing, rule), List.of()));
        if (second.find(existingId).isPresent() || second.find(missingId).isPresent()
                || !second.byId().isEmpty()) {
            helper.fail("Recipe reload scan retained stale or missing Recipe entries");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/processing/brass_mixing", templateNamespace = CreateProbabilityTuning.MOD_ID,
            timeoutTicks = 160)
    public static void brassMixingCapturesFailureBeforeExtraction(GameTestHelper helper) {
        helper.runAtTickTime(20, () -> {
            BasinBlockEntity basin = null;
            for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 8, 8, 8)) {
                var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
                if (blockEntity instanceof BasinBlockEntity found) {
                    basin = found;
                    break;
                }
            }
            if (basin == null) {
                helper.fail("No BasinBlockEntity found in brass mixing structure");
                return;
            }
            var recipe = helper.getLevel().getRecipeManager()
                    .byKey(net.minecraft.resources.ResourceLocation.parse("create:mixing/brass_ingot"))
                    .orElseThrow()
                    .value();
            var recipeId = net.minecraft.resources.ResourceLocation.parse("create:mixing/brass_ingot");
            CreateHook.active().replaceIndex(new RecipeIndex(Map.of(recipeId, new RecipeContext(recipeId, recipe,
                    new RecipeRule(true, 0.0, RecipeRule.Mode.DISCRETE, List.of(0.0), "")) )));
            if (!insertIngredients(basin, recipe)) {
                helper.fail("Could not insert first ingredient set into Basin input inventory");
                return;
            }
            if (!BasinRecipe.apply(basin, recipe)) {
                helper.fail("Create BasinRecipe.apply returned false");
                return;
            }
            var decision = CreateHook.active().lastDecision();
            if (decision == null || decision.status() != com.pycoder.createprobabilitytuning.recipe.ProcessingDecision.Status.FAILURE
                    || com.pycoder.createprobabilitytuning.nbt.ProcessData.readAttempts(decision.input(), recipeId) != 1) {
                helper.fail("Expected first direct Create failure with Attempts=1, got " + decision);
                return;
            }
            if (decision.outputs().isEmpty() || ProcessData.readAttempts(decision.outputs().getFirst(), recipeId) != 1) {
                helper.fail("Expected Basin failure output to retain Attempts=1, got " + decision.outputs());
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/processing/brass_mixing", templateNamespace = CreateProbabilityTuning.MOD_ID,
            timeoutTicks = 160)
    public static void sequencedAssemblyEvaluatesFinalStepOnce(GameTestHelper helper) {
        // CreateHook is a process-wide adapter; run this stateful integration
        // test after the other tests that replace its test index have finished.
        helper.runAtTickTime(40, () -> {
            var parentId = net.minecraft.resources.ResourceLocation.parse("create:sequenced_assembly/precision_mechanism");
            var parent = helper.getLevel().getRecipeManager().byKey(parentId)
                    .map(holder -> holder.value())
                    .filter(SequencedAssemblyRecipe.class::isInstance)
                    .map(SequencedAssemblyRecipe.class::cast)
                    .orElseThrow();
            int finalStep = parent.getLoops() * parent.getSequence().size() - 1;
            var input = parent.getTransitionalItem().copyWithCount(2);
            input.set(AllDataComponents.SEQUENCED_ASSEMBLY,
                    new SequencedAssemblyRecipe.SequencedAssembly(parentId, finalStep,
                            (float) finalStep / (parent.getLoops() * parent.getSequence().size())));
            var stepRecipe = parent.getSequence().get(finalStep % parent.getSequence().size()).getRecipe();
            CreateHook.active().replaceIndex(new RecipeIndex(Map.of(parentId, new RecipeContext(parentId, parent,
                    new RecipeRule(true, 0.0, RecipeRule.Mode.DISCRETE, List.of(0.0), "")) )));
            @SuppressWarnings("unchecked")
            net.minecraft.world.item.crafting.RecipeHolder<com.simibubi.create.content.processing.recipe.ProcessingRecipe<?, ?>> forced =
                (net.minecraft.world.item.crafting.RecipeHolder<com.simibubi.create.content.processing.recipe.ProcessingRecipe<?, ?>>)
                    (net.minecraft.world.item.crafting.RecipeHolder<?>) SequencedAssemblyRecipe.getRecipe(helper.getLevel(), input,
                    (net.minecraft.world.item.crafting.RecipeType) stepRecipe.getType(),
                    (Class) com.simibubi.create.content.processing.recipe.ProcessingRecipe.class).orElse(null);
            if (forced == null) {
                helper.fail("Create did not resolve the final Sequenced Assembly step");
                return;
            }
            var outputs = forced.value().rollResults(RandomSource.create(1L));
            if (outputs.size() != 1 || !parent.getIngredient().test(outputs.getFirst())
                    || outputs.getFirst().has(AllDataComponents.SEQUENCED_ASSEMBLY)
                    || ProcessData.readAttempts(outputs.getFirst(), parentId) != 1) {
                helper.fail("Expected final-step failure to return parent ingredient with Attempts=1, outputs=" + outputs);
                return;
            }
            input = parent.getTransitionalItem().copyWithCount(1);
            input.set(AllDataComponents.SEQUENCED_ASSEMBLY,
                    new SequencedAssemblyRecipe.SequencedAssembly(parentId, finalStep,
                            (float) finalStep / (parent.getLoops() * parent.getSequence().size())));
            List<ItemStack> appliedOutputs = RecipeApplier.applyRecipeOn(
                    helper.getLevel(), input, forced.value(), false);
            if (appliedOutputs.size() != 1 || !parent.getIngredient().test(appliedOutputs.getFirst())
                    || ProcessData.readAttempts(appliedOutputs.getFirst(), parentId) != 1) {
                helper.fail("Expected RecipeApplier final-step failure to preserve Attempts=1, outputs=" + appliedOutputs);
                return;
            }
            ItemStack previousFailure = outputs.getFirst();
            for (int expectedAttempts = 2; expectedAttempts <= 3; expectedAttempts++) {
                input = parent.getTransitionalItem().copyWithCount(1);
                ProcessData.copyModData(previousFailure, input);
                input.set(AllDataComponents.SEQUENCED_ASSEMBLY,
                        new SequencedAssemblyRecipe.SequencedAssembly(parentId, finalStep,
                                (float) finalStep / (parent.getLoops() * parent.getSequence().size())));
                CreateHook.active().replaceIndex(new RecipeIndex(Map.of(parentId, new RecipeContext(parentId, parent,
                        new RecipeRule(true, 0.0, RecipeRule.Mode.DISCRETE, List.of(0.0), "")) )));
                forced = (net.minecraft.world.item.crafting.RecipeHolder<com.simibubi.create.content.processing.recipe.ProcessingRecipe<?, ?>>)
                        (net.minecraft.world.item.crafting.RecipeHolder<?>) SequencedAssemblyRecipe.getRecipe(helper.getLevel(), input,
                        (net.minecraft.world.item.crafting.RecipeType) stepRecipe.getType(),
                        (Class) com.simibubi.create.content.processing.recipe.ProcessingRecipe.class).orElse(null);
                if (forced == null) {
                    helper.fail("Create did not resolve repeated final Sequenced Assembly failure " + expectedAttempts);
                    return;
                }
                outputs = forced.value().rollResults(RandomSource.create(expectedAttempts));
                if (outputs.size() != 1 || ProcessData.readAttempts(outputs.getFirst(), parentId) != expectedAttempts) {
                    helper.fail("Expected repeated final failure to preserve Attempts=" + expectedAttempts
                            + ", outputs=" + outputs);
                    return;
                }
                previousFailure = outputs.getFirst();
            }
            CreateHook.active().replaceIndex(new RecipeIndex(Map.of(parentId, new RecipeContext(parentId, parent,
                    new RecipeRule(true, 1.0, RecipeRule.Mode.DISCRETE, List.of(0.0), "")) )));
            input = parent.getTransitionalItem().copyWithCount(1);
            input.set(AllDataComponents.SEQUENCED_ASSEMBLY,
                    new SequencedAssemblyRecipe.SequencedAssembly(parentId, finalStep,
                            (float) finalStep / (parent.getLoops() * parent.getSequence().size())));
            forced = (net.minecraft.world.item.crafting.RecipeHolder<com.simibubi.create.content.processing.recipe.ProcessingRecipe<?, ?>>)
                    (net.minecraft.world.item.crafting.RecipeHolder<?>) SequencedAssemblyRecipe.getRecipe(helper.getLevel(), input,
                    (net.minecraft.world.item.crafting.RecipeType) stepRecipe.getType(),
                    (Class) com.simibubi.create.content.processing.recipe.ProcessingRecipe.class).orElse(null);
            if (forced == null) {
                helper.fail("Create did not resolve the final Sequenced Assembly success step");
                return;
            }
            outputs = forced.value().rollResults(RandomSource.create(2L));
            if (outputs.size() != 1 || parent.getIngredient().test(outputs.getFirst())
                    || outputs.getFirst().has(AllDataComponents.SEQUENCED_ASSEMBLY)) {
                helper.fail("Expected successful sequence to select exactly one result-pool output, outputs=" + outputs);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/processing/brass_mixing", templateNamespace = CreateProbabilityTuning.MOD_ID)
    public static void resultPoolUsesOneExclusiveOutcome(GameTestHelper helper) {
        helper.runAtTickTime(20, () -> {
            var override = new RecipeOverride();
            var mainAndZeroExtra = List.of(
                    new ProcessingOutput(Items.COPPER_INGOT, 1, 1.0f),
                    new ProcessingOutput(Items.IRON_NUGGET, 1, 0.0f));
            var success = override.evaluate(
                    new RecipeRule(true, 1.0, RecipeRule.Mode.DISCRETE, List.of(0.0), ""),
                    mainAndZeroExtra, Items.GOLD_INGOT.getDefaultInstance(), RandomSource.create(1L));
            if (success.status() != com.pycoder.createprobabilitytuning.recipe.ProcessingDecision.Status.SUCCESS
                    || success.outputs().size() != 1
                    || (!success.outputs().getFirst().is(Items.COPPER_INGOT)
                    && !success.outputs().getFirst().is(Items.IRON_NUGGET))) {
                helper.fail("Expected one exclusive eligible output, got " + success);
                return;
            }

            var mainAndGuaranteedExtra = List.of(
                    new ProcessingOutput(Items.COPPER_INGOT, 1, 1.0f),
                    new ProcessingOutput(Items.IRON_NUGGET, 1, 1.0f));
            var failure = override.evaluate(
                    new RecipeRule(true, 0.0, RecipeRule.Mode.DISCRETE, List.of(0.0), ""),
                    mainAndGuaranteedExtra, Items.GOLD_INGOT.getDefaultInstance(), RandomSource.create(1L));
            if (failure.status() != com.pycoder.createprobabilitytuning.recipe.ProcessingDecision.Status.FAILURE
                    || failure.outputs().size() != 1
                    || !failure.outputs().getFirst().is(Items.GOLD_INGOT)
                    || ProcessData.readAttempts(failure.outputs().getFirst()) != 1) {
                helper.fail("Expected CPT fallback instead of an independent extra output, got " + failure);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/processing/brass_mixing", templateNamespace = CreateProbabilityTuning.MOD_ID)
    public static void weightedPoolRenormalizesOriginalFallback(GameTestHelper helper) {
        helper.runAtTickTime(20, () -> {
            var override = new RecipeOverride();
            var main = new ProcessingOutput(Items.DIAMOND, 1, 8.0f);
            var originalFallback = new ProcessingOutput(Items.IRON_INGOT, 1, 1.0f);
            var byproduct = new ProcessingOutput(Items.GOLD_NUGGET, 1, 1.0f);
            var outputs = List.of(main, originalFallback, byproduct);
            var rule = new RecipeRule(true, 0.5, RecipeRule.Mode.DISCRETE, List.of(0.0), "");
            var fallback = (java.util.function.Predicate<ItemStack>) stack -> stack.is(Items.IRON_INGOT);
            double mainChance = override.calculateWeightedChance(rule, outputs, fallback, main, 0);
            double extraChance = override.calculateWeightedChance(rule, outputs, fallback, byproduct, 0);
            double fallbackChance = override.calculateWeightedFallbackChance(rule, outputs, fallback, 0);
            if (Math.abs(mainChance - 4.0 / 9.0) > 1.0e-6
                    || Math.abs(extraChance - 1.0 / 18.0) > 1.0e-6
                    || Math.abs(mainChance + extraChance + fallbackChance - 1.0) > 1.0e-6) {
                helper.fail("Weighted pool probabilities were not normalized: main=" + mainChance
                        + ", extra=" + extraChance + ", fallback=" + fallbackChance);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/processing/brass_mixing", templateNamespace = CreateProbabilityTuning.MOD_ID)
    public static void directProcessingUsesConfiguredProbability(GameTestHelper helper) {
        helper.runAtTickTime(20, () -> {
            var recipeId = net.minecraft.resources.ResourceLocation.parse("create:pressing/iron_ingot");
            var recipe = helper.getLevel().getRecipeManager().byKey(recipeId)
                    .orElseThrow()
                    .value();
            ItemStack input = Items.IRON_INGOT.getDefaultInstance();
            CreateHook.active().replaceIndex(new RecipeIndex(Map.of(recipeId, new RecipeContext(recipeId, recipe,
                    new RecipeRule(true, 0.0, RecipeRule.Mode.DISCRETE, List.of(0.0), "")) )));
            CreateHook.active().replaceJeiIndex(new RecipeIndex(Map.of()));

            List<ItemStack> outputs = RecipeApplier.applyRecipeOn(helper.getLevel(), input, recipe, false);
            if (outputs.size() != 1 || outputs.getFirst().getCount() != 1
                    || !outputs.getFirst().is(Items.IRON_INGOT)
                    || ProcessData.readAttempts(outputs.getFirst(), recipeId) != 1) {
                helper.fail("Expected direct Create processing to return retry iron ingot with Attempts=1, outputs="
                        + outputs + ", input=" + input);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/processing/brass_mixing", templateNamespace = CreateProbabilityTuning.MOD_ID)
    public static void maxNConsumesFinalFailedAttempt(GameTestHelper helper) {
        helper.runAtTickTime(20, () -> {
            var recipeId = net.minecraft.resources.ResourceLocation.parse("create:pressing/iron_ingot");
            var recipe = helper.getLevel().getRecipeManager().byKey(recipeId).orElseThrow().value();
            ItemStack input = Items.IRON_INGOT.getDefaultInstance();
            CreateHook.active().replaceIndex(new RecipeIndex(Map.of(recipeId, new RecipeContext(recipeId, recipe,
                    new RecipeRule(true, 0.0, RecipeRule.Mode.DISCRETE, List.of(0.0), "", 1)))));

            List<ItemStack> outputs = RecipeApplier.applyRecipeOn(helper.getLevel(), input, recipe, false);
            if (!outputs.isEmpty() || ProcessData.readAttempts(input, recipeId) != 0) {
                helper.fail("Expected max_n=1 to consume failed input without mutating source, outputs=" + outputs + ", input=" + input);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/processing/brass_mixing", templateNamespace = CreateProbabilityTuning.MOD_ID)
    public static void batchProcessingDoesNotShareFailureHistory(GameTestHelper helper) {
        helper.runAtTickTime(20, () -> {
            var recipeId = net.minecraft.resources.ResourceLocation.parse("create:pressing/iron_ingot");
            var recipe = helper.getLevel().getRecipeManager().byKey(recipeId).orElseThrow().value();
            ItemStack input = Items.IRON_INGOT.getDefaultInstance();
            input.setCount(8);
            CreateHook.active().replaceIndex(new RecipeIndex(Map.of(recipeId, new RecipeContext(recipeId, recipe,
                    new RecipeRule(true, 0.0, RecipeRule.Mode.DISCRETE, List.of(0.0), "")) )));

            List<ItemStack> outputs = RecipeApplier.applyRecipeOn(helper.getLevel(), input, recipe, false);
            if (outputs.size() != 1 || outputs.getFirst().getCount() != 8
                    || ProcessData.readAttempts(outputs.getFirst(), recipeId) != 1
                    || ProcessData.readAttempts(input, recipeId) != 0) {
                helper.fail("Each batch item must independently return Attempts=1, outputs=" + outputs
                        + ", input=" + input);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/processing/brass_mixing", templateNamespace = CreateProbabilityTuning.MOD_ID)
    public static void maxNSequencedBoundaries(GameTestHelper helper) {
        helper.runAtTickTime(20, () -> {
            var override = new RecipeOverride();
            var recipeId = net.minecraft.resources.ResourceLocation.parse("create:test_sequence_max_n");
            var pool = List.of(new ProcessingOutput(Items.IRON_INGOT, 1, 1.0f));
            for (int maxN : new int[] {1, 2, 5, 7}) {
                var rule = new RecipeRule(true, 0.0, RecipeRule.Mode.DISCRETE, List.of(0.0), "", maxN);
                ItemStack input = Items.IRON_INGOT.getDefaultInstance();
                for (int attempt = 1; attempt <= maxN; attempt++) {
                    var decision = override.evaluateSequenced(recipeId, rule, pool, input,
                            RandomSource.create(attempt));
                    if (attempt < maxN) {
                        if (!decision.returnsInput() || decision.outputs().size() != 1) {
                            helper.fail("max_n=" + maxN + " consumed too early at attempt " + attempt);
                            return;
                        }
                        input = decision.outputs().getFirst();
                        if (ProcessData.readAttempts(input, recipeId) != attempt) {
                            helper.fail("max_n=" + maxN + " lost attempt " + attempt);
                            return;
                        }
                    } else if (decision.returnsInput() || !decision.outputs().isEmpty()) {
                        helper.fail("max_n=" + maxN + " did not consume at attempt " + attempt);
                        return;
                    }
                }
            }
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/processing/brass_mixing", templateNamespace = CreateProbabilityTuning.MOD_ID)
    public static void maxNFailureWithExtraConsumesWithoutIndependentByproduct(GameTestHelper helper) {
        helper.runAtTickTime(20, () -> {
            var outputs = List.of(
                    new ProcessingOutput(Items.COPPER_INGOT, 1, 1.0f),
                    new ProcessingOutput(Items.IRON_NUGGET, 1, 1.0f));
            var decision = new RecipeOverride().evaluate(
                    new RecipeRule(true, 0.0, RecipeRule.Mode.DISCRETE, List.of(0.0), "", 1),
                    outputs, Items.GOLD_INGOT.getDefaultInstance(), RandomSource.create(1L));
            if (decision.returnsInput() || !decision.outputs().isEmpty()) {
                helper.fail("Terminal max_n failure must not emit an independent byproduct: " + decision);
                return;
            }
            helper.succeed();
        });
    }

    private static BasinBlockEntity findBasin(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 8, 8, 8)) {
            var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            if (blockEntity instanceof BasinBlockEntity basin) {
                return basin;
            }
        }
        return null;
    }

    private static String insertSequencedIngredients(BasinBlockEntity basin,
                                                     net.minecraft.world.item.crafting.Recipe<?> recipe,
                                                     ItemStack transitional) {
        for (int slot = 0; slot < basin.getInputInventory().getSlots(); slot++) {
            basin.getInputInventory().setStackInSlot(slot, ItemStack.EMPTY);
        }
        int index = 0;
        for (var ingredient : recipe.getIngredients()) {
            ItemStack remaining = (index++ == 0 ? transitional : ingredient.getItems()[0].copyWithCount(2));
            int slot = index - 1;
            if (slot >= basin.getInputInventory().getSlots()) {
                return "not enough Basin input slots for ingredient=" + (index - 1);
            }
            basin.getInputInventory().setStackInSlot(slot, remaining);
            remaining = ItemStack.EMPTY;
            if (!remaining.isEmpty()) {
                return "ingredient=" + (index - 1) + ", item=" + remaining.getItem()
                        + ", count=" + remaining.getCount() + ", type=" + ingredient.getClass().getName();
            }
        }
        return null;
    }

    private static boolean insertIngredients(BasinBlockEntity basin, net.minecraft.world.item.crafting.Recipe<?> recipe) {
        return insertIngredients(basin, recipe, ItemStack.EMPTY);
    }

    private static boolean insertIngredients(BasinBlockEntity basin, net.minecraft.world.item.crafting.Recipe<?> recipe,
                                             ItemStack firstIngredient) {
        for (int slot = 0; slot < basin.getInputInventory().getSlots(); slot++) {
            basin.getInputInventory().setStackInSlot(slot, ItemStack.EMPTY);
        }
        int index = 0;
        for (var ingredient : recipe.getIngredients()) {
            ItemStack remaining = index++ == 0 && !firstIngredient.isEmpty()
                    ? firstIngredient.copy()
                    : ingredient.getItems()[0].copy();
            remaining.setCount(3);
            int slot = index - 1;
            if (slot >= basin.getInputInventory().getSlots()) {
                return false;
            }
            basin.getInputInventory().setStackInSlot(slot, remaining);
        }
        return true;
    }

}
