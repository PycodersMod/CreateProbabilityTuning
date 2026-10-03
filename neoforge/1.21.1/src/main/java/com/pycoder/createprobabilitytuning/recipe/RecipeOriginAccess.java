package com.pycoder.createprobabilitytuning.recipe;

import net.minecraft.resources.ResourceLocation;

/** 显式标记从其他配方重建出的 Create 配方的来源身份。 */
public interface RecipeOriginAccess {
    ResourceLocation cpt$getOrigin();

    void cpt$setOrigin(ResourceLocation id);
}
