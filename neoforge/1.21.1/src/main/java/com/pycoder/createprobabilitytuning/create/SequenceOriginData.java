package com.pycoder.createprobabilitytuning.create;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.Logger;

import java.util.Optional;

/** 保存确切的序列起始物品堆，避免猜测标签中的第一个物品。 */
public final class SequenceOriginData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NAMESPACE = "CreateProbabilityTuning";
    private static final String KEY = "SequenceOrigin";
    private static final String RECIPE = "Recipe";
    private static final String STACK = "Stack";

    private SequenceOriginData() {
    }

    public static boolean has(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        return root.contains(NAMESPACE, 10)
                && root.getCompound(NAMESPACE).contains(KEY, 10);
    }

    public static void capture(ItemStack target, ResourceLocation recipeId,
                               ItemStack origin, HolderLookup.Provider registries) {
        if (target.isEmpty() || origin.isEmpty() || has(target)) {
            return;
        }
        CompoundTag encoded = encode(origin, registries);
        if (encoded == null) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, target, root -> {
            CompoundTag mod = root.contains(NAMESPACE, 10) ? root.getCompound(NAMESPACE) : new CompoundTag();
            CompoundTag data = new CompoundTag();
            data.putString(RECIPE, recipeId.toString());
            data.put(STACK, encoded);
            mod.put(KEY, data);
            root.put(NAMESPACE, mod);
        });
    }

    public static Optional<ItemStack> read(ItemStack source, ResourceLocation recipeId,
                                           HolderLookup.Provider registries) {
        CompoundTag root = source.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        if (!root.contains(NAMESPACE, 10)) {
            return Optional.empty();
        }
        CompoundTag mod = root.getCompound(NAMESPACE);
        if (!mod.contains(KEY, 10)) {
            return Optional.empty();
        }
        CompoundTag data = mod.getCompound(KEY);
        if (!recipeId.toString().equals(data.getString(RECIPE)) || !data.contains(STACK, 10)) {
            return Optional.empty();
        }
        return decode(data.getCompound(STACK), registries);
    }

    public static void clear(ItemStack target) {
        CustomData.update(DataComponents.CUSTOM_DATA, target, root -> {
            if (!root.contains(NAMESPACE, 10)) return;
            CompoundTag mod = root.getCompound(NAMESPACE);
            mod.remove(KEY);
            if (mod.isEmpty()) root.remove(NAMESPACE);
            else root.put(NAMESPACE, mod);
        });
    }

    public static void copy(ItemStack source, ItemStack target) {
        CompoundTag root = source.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        if (!root.contains(NAMESPACE, 10) || !root.getCompound(NAMESPACE).contains(KEY, 10)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, target, output -> {
            CompoundTag mod = output.contains(NAMESPACE, 10) ? output.getCompound(NAMESPACE) : new CompoundTag();
            mod.put(KEY, root.getCompound(NAMESPACE).getCompound(KEY).copy());
            output.put(NAMESPACE, mod);
        });
    }

    private static CompoundTag encode(ItemStack stack, HolderLookup.Provider registries) {
        try {
            return (CompoundTag) ItemStack.CODEC.encodeStart(
                    net.minecraft.resources.RegistryOps.create(NbtOps.INSTANCE, registries),
                    stack.copyWithCount(1)).resultOrPartial(LOGGER::error).orElse(null);
        } catch (RuntimeException exception) {
            LOGGER.error("Unable to save sequence origin stack", exception);
            return null;
        }
    }

    private static Optional<ItemStack> decode(CompoundTag tag, HolderLookup.Provider registries) {
        try {
            return ItemStack.CODEC.parse(
                    net.minecraft.resources.RegistryOps.create(NbtOps.INSTANCE, registries), tag)
                    .resultOrPartial(LOGGER::error);
        } catch (RuntimeException exception) {
            LOGGER.error("Unable to restore sequence origin stack", exception);
            return Optional.empty();
        }
    }
}
