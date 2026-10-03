package com.pycoder.createprobabilitytuning.mixin;

import com.pycoder.createprobabilitytuning.create.CreateHook;
import com.pycoder.createprobabilitytuning.create.CreateOutputBridge;
import com.pycoder.createprobabilitytuning.recipe.ProcessingDecision;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BasinRecipe.class)
public abstract class BasinRecipeMixin {
    @Unique
    private static final ThreadLocal<Invocation> CPT$INVOCATION = new ThreadLocal<>();

    @Inject(method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)Z", at = @At("HEAD"))
    private static void cpt$enter(BasinBlockEntity basin, Recipe<?> recipe, boolean simulate,
                                  CallbackInfoReturnable<Boolean> callback) {
        CreateHook hook = CreateHook.active();
        ProcessingDecision prepared = !simulate && hook != null ? hook.prepareBasinDecision(basin, recipe) : null;
        CPT$INVOCATION.set(new Invocation(prepared));
    }

    @Inject(method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)Z", at = @At("RETURN"))
    private static void cpt$exit(BasinBlockEntity basin, Recipe<?> recipe, boolean simulate,
                                 CallbackInfoReturnable<Boolean> callback) {
        CPT$INVOCATION.remove();
    }

    @Redirect(
            method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;acceptOutputs(Ljava/util/List;Ljava/util/List;Z)Z"))
    private static boolean cpt$acceptOutputs(BasinBlockEntity basin, List<ItemStack> items,
                                             List<FluidStack> fluids, boolean simulate) {
        Invocation invocation = CPT$INVOCATION.get();
        CreateHook hook = CreateHook.active();
        if (invocation == null || hook == null) {
            return basin.acceptOutputs(items, fluids, simulate);
        }
        // 在 Create 的模拟容量检查阶段和实际提取阶段使用相同的已选输出。
        // 若模拟阶段仍使用 Create 的原始输出，容量检查可能与真实概率结果不一致，
        // 并在错误的检查结果下消耗输入。
        List<ItemStack> resolved = invocation.prepared() == null
                ? items
                : CreateOutputBridge.resolve(items, invocation.prepared().status(), invocation.prepared().outputs());
        return basin.acceptOutputs(resolved, fluids, simulate);
    }

    @Unique
    private record Invocation(ProcessingDecision prepared) {
    }
}
