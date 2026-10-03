package com.pycoder.createprobabilitytuning.mixin;

import com.pycoder.createprobabilitytuning.create.CreateHook;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.utility.CreateLang;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 在所有加工类别中复用 Create 自带的概率槽位纹理。 */
@Mixin(CreateRecipeCategory.class)
public abstract class CreateRecipeCategoryMixin {
    /** @reason 将已配置的概率应用到 Create 现有的 JEI 概率槽位。 */
    @Inject(method = "getRenderedSlot(Lcom/simibubi/create/content/processing/recipe/ProcessingOutput;)Lmezz/jei/api/gui/drawable/IDrawable;",
            at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private static void cpt$getRenderedSlot(ProcessingOutput output, CallbackInfoReturnable<IDrawable> callback) {
        CreateHook hook = CreateHook.active();
        float chance = hook == null ? output.getChance() : hook.configuredChance(output, output.getChance());
        callback.setReturnValue(CreateRecipeCategory.getRenderedSlot(chance));
    }

    /** @reason 保留 Create 原有的概率提示文本，但使用已配置的概率值。 */
    @Inject(method = "addStochasticTooltip(Lcom/simibubi/create/content/processing/recipe/ProcessingOutput;)Lmezz/jei/api/gui/ingredient/IRecipeSlotRichTooltipCallback;",
            at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private static void cpt$addStochasticTooltip(ProcessingOutput output,
                                                  CallbackInfoReturnable<IRecipeSlotRichTooltipCallback> callback) {
        callback.setReturnValue((view, tooltip) -> {
            CreateHook hook = CreateHook.active();
            float chance = hook == null ? output.getChance() : hook.configuredChance(output, output.getChance());
            if (chance != 1) {
                tooltip.add(CreateLang.translateDirect("recipe.processing.chance",
                        chance < 0.01 ? "<1" : (int) (chance * 100)).withStyle(ChatFormatting.GOLD));
            }
        });
    }
}
