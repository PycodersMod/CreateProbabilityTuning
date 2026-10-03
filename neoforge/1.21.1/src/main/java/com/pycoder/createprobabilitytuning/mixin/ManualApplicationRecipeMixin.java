package com.pycoder.createprobabilitytuning.mixin;

import com.pycoder.createprobabilitytuning.create.CreateHook;
import com.pycoder.createprobabilitytuning.create.CreateOutputBridge;
import com.pycoder.createprobabilitytuning.recipe.ProcessingDecision;
import com.pycoder.createprobabilitytuning.nbt.PlacedProcessData;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 覆盖 Create 中玩家右键应用物品的路径。
 *
 * Create 将此路径分为两个阶段：transformBlock 放置主要方块，随后 rollResults 掉落额外输出。
 * 在第一阶段作出判定，并在第二阶段使用该结果，避免失败操作留下变换后的方块或重复抽取概率。
 */
@Mixin(ManualApplicationRecipe.class)
public abstract class ManualApplicationRecipeMixin {
    @Unique
    private static final ThreadLocal<Invocation> CPT$INVOCATION = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock> CPT$EVENT = new ThreadLocal<>();

    @Inject(
            method = "manualApplicationRecipesApplyInWorld(Lnet/neoforged/neoforge/event/entity/player/PlayerInteractEvent$RightClickBlock;)V",
            at = @At("HEAD"))
    private static void cpt$manualApplicationEnter(
            net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event,
            CallbackInfo callback) {
        CPT$EVENT.set(event);
    }

    @Redirect(
            method = "manualApplicationRecipesApplyInWorld(Lnet/neoforged/neoforge/event/entity/player/PlayerInteractEvent$RightClickBlock;)V",
            at = @At(value = "INVOKE", target =
                    "Lcom/simibubi/create/content/kinetics/deployer/ManualApplicationRecipe;transformBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState cpt$manualApplicationTransform(ManualApplicationRecipe recipe,
                                                              BlockState inputState,
                                                              RandomSource random) {
        CPT$INVOCATION.remove();
        CreateHook hook = CreateHook.active();
        if (hook == null || inputState.isAir() || inputState.getBlock().asItem() == net.minecraft.world.item.Items.AIR) {
            return recipe.transformBlock(inputState, random);
        }

        var event = CPT$EVENT.get();
        Level level = event == null ? null : event.getLevel();
        ItemStack input = inputState.getBlock().asItem() == net.minecraft.world.item.Items.AIR
                ? ItemStack.EMPTY
                : level instanceof ServerLevel serverLevel
                ? PlacedProcessData.get(serverLevel).get(event.getPos(), inputState.getBlock().asItem())
                : ItemStack.EMPTY;
        if (input.isEmpty()) {
            input = new ItemStack(inputState.getBlock().asItem());
        }
        ProcessingDecision decision = level == null
                ? hook.handleFinalProcessing(recipe, input, recipe.getRollableResults(), random)
                : hook.handleFinalProcessing(level, recipe, input, recipe.getRollableResults(), random);
        if (decision.status() == ProcessingDecision.Status.DISABLED
                || decision.status() == ProcessingDecision.Status.UNAVAILABLE) {
            return recipe.transformBlock(inputState, random);
        }
        CPT$INVOCATION.set(new Invocation(decision, event, input));

        if (decision.status() == ProcessingDecision.Status.FAILURE && decision.returnsInput()) {
            return inputState;
        }
        if (decision.status() != ProcessingDecision.Status.SUCCESS || decision.outputs().isEmpty()) {
            return Blocks.AIR.defaultBlockState();
        }
        ItemStack selected = decision.outputs().getFirst();
        if (!(selected.getItem() instanceof BlockItem blockItem)) {
            return Blocks.AIR.defaultBlockState();
        }
        return BlockHelper.copyProperties(inputState, blockItem.getBlock().defaultBlockState());
    }

    @Redirect(
            method = "manualApplicationRecipesApplyInWorld(Lnet/neoforged/neoforge/event/entity/player/PlayerInteractEvent$RightClickBlock;)V",
            at = @At(value = "INVOKE", target =
                    "Lcom/simibubi/create/content/kinetics/deployer/ManualApplicationRecipe;rollResults(Lnet/minecraft/util/RandomSource;)Ljava/util/List;"))
    private static List<ItemStack> cpt$manualApplicationResults(ManualApplicationRecipe recipe,
                                                                  RandomSource random) {
        Invocation invocation = CPT$INVOCATION.get();
        if (invocation == null) {
            return recipe.rollResults(random);
        }
        if (invocation.event() != null && invocation.event().getLevel() instanceof ServerLevel level) {
            PlacedProcessData placed = PlacedProcessData.get(level);
            if (invocation.decision().status() == ProcessingDecision.Status.SUCCESS
                    || !invocation.decision().returnsInput()) {
                placed.remove(invocation.event().getPos());
            } else if (!invocation.decision().outputs().isEmpty()) {
                placed.put(invocation.event().getPos(), invocation.decision().outputs().getFirst());
            }
        }
        if (invocation.decision().status() == ProcessingDecision.Status.SUCCESS
                && !invocation.decision().outputs().isEmpty()
                && invocation.decision().outputs().getFirst().getItem() instanceof BlockItem) {
            // 已选中的方块已由 transformBlock 放置，不要再将其作为物品掉落。
            return List.of();
        }
        return CreateOutputBridge.resolve(
                List.of(), invocation.decision().status(), invocation.decision().outputs());
    }

    @Inject(
            method = "manualApplicationRecipesApplyInWorld(Lnet/neoforged/neoforge/event/entity/player/PlayerInteractEvent$RightClickBlock;)V",
            at = @At("RETURN"))
    private static void cpt$manualApplicationExit(
            net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event,
            CallbackInfo callback) {
        CPT$INVOCATION.remove();
        CPT$EVENT.remove();
    }

    @Unique
    private static void cpt$clearManualApplicationContext() {
        CPT$INVOCATION.remove();
    }

    @Unique
    private record Invocation(ProcessingDecision decision,
                              net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event,
                              ItemStack input) {
    }
}
