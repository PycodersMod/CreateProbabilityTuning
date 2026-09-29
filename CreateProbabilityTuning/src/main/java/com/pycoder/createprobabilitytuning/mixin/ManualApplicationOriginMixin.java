package com.pycoder.createprobabilitytuning.mixin;

import com.pycoder.createprobabilitytuning.recipe.RecipeOriginAccess;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ManualApplicationRecipe.class)
public abstract class ManualApplicationOriginMixin {
    @Inject(method = "asDeploying(Lnet/minecraft/world/item/crafting/RecipeHolder;)Lnet/minecraft/world/item/crafting/RecipeHolder;",
            at = @At("RETURN"))
    private static void cpt$setExplicitOrigin(RecipeHolder<?> source,
                                                CallbackInfoReturnable<RecipeHolder<DeployerApplicationRecipe>> callback) {
        RecipeHolder<DeployerApplicationRecipe> result = callback.getReturnValue();
        if (result.value() instanceof RecipeOriginAccess access) {
            access.cpt$setOrigin(source.id());
        }
    }
}
