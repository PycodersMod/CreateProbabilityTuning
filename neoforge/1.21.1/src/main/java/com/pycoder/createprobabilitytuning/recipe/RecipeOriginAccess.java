package com.pycoder.createprobabilitytuning.recipe;

import net.minecraft.resources.ResourceLocation;

/** Explicit source identity for a Create recipe reconstructed from another recipe. */
public interface RecipeOriginAccess {
    ResourceLocation cpt$getOrigin();

    void cpt$setOrigin(ResourceLocation id);
}
