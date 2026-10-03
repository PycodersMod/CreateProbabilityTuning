package com.pycoder.createprobabilitytuning.mixin;

import com.pycoder.createprobabilitytuning.recipe.RecipeOriginAccess;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(DeployerApplicationRecipe.class)
public abstract class DeployerApplicationRecipeMixin implements RecipeOriginAccess {
    @Unique
    private ResourceLocation cpt$origin;

    @Override
    public ResourceLocation cpt$getOrigin() {
        return cpt$origin;
    }

    @Override
    public void cpt$setOrigin(ResourceLocation id) {
        cpt$origin = id;
    }
}
