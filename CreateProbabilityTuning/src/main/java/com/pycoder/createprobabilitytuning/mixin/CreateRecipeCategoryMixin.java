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

/** Reuses Create's own chance-slot texture for every processing category. */
@Mixin(CreateRecipeCategory.class)
public abstract class CreateRecipeCategoryMixin {
    /** @reason Apply the configured chance to Create's existing JEI chance slot. */
    @Inject(method = "getRenderedSlot(Lcom/simibubi/create/content/processing/recipe/ProcessingOutput;)Lmezz/jei/api/gui/drawable/IDrawable;",
            at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private static void cpt$getRenderedSlot(ProcessingOutput output, CallbackInfoReturnable<IDrawable> callback) {
        CreateHook hook = CreateHook.active();
        float chance = hook == null ? output.getChance() : hook.configuredChance(output, output.getChance());
        callback.setReturnValue(CreateRecipeCategory.getRenderedSlot(chance));
    }

    /** @reason Keep Create's original chance tooltip, but use the configured chance. */
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
