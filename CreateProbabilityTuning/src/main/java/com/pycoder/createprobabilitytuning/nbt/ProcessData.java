package com.pycoder.createprobabilitytuning.nbt;

import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.Logger;

/** Reads and writes only this mod's failure history in an ItemStack. */
public final class ProcessData {
    public static final String NAMESPACE = "CreateProbabilityTuning";
    public static final String ATTEMPTS = "Attempts";
    private static final String LEGACY_RECIPE = "__legacy__";
    private static final Logger LOGGER = LogUtils.getLogger();

    private ProcessData() {
    }

    public static int readAttempts(ItemStack stack) {
        return readAttempts(customData(stack));
    }

    public static int readAttempts(ItemStack stack, ResourceLocation recipeId) {
        return readAttempts(customData(stack), recipeId);
    }

    public static int readAttempts(CompoundTag root) {
        if (!root.contains(NAMESPACE, 10)) {
            return 0;
        }
        CompoundTag modData = root.getCompound(NAMESPACE);
        if (modData.contains(ATTEMPTS, 3)) {
            return nonNegative(modData.getInt(ATTEMPTS));
        }
        if (!modData.contains(ATTEMPTS, 10)) {
            return 0;
        }
        return maxAttempts(modData.getCompound(ATTEMPTS));
    }

    public static int readAttempts(CompoundTag root, ResourceLocation recipeId) {
        if (!root.contains(NAMESPACE, 10)) {
            return 0;
        }
        CompoundTag modData = root.getCompound(NAMESPACE);
        return modData.contains(ATTEMPTS, 3)
                ? nonNegative(modData.getInt(ATTEMPTS))
                : readAttempts(modData, recipeId.toString());
    }

    private static int readAttempts(CompoundTag modData, String recipeId) {
        if (!modData.contains(ATTEMPTS, 10)) {
            return 0;
        }
        CompoundTag attempts = modData.getCompound(ATTEMPTS);
        if (attempts.contains(recipeId, 3)) {
            return nonNegative(attempts.getInt(recipeId));
        }
        return LEGACY_RECIPE.equals(recipeId) || attempts.isEmpty()
                ? 0
                : attempts.contains(LEGACY_RECIPE, 3)
                ? nonNegative(attempts.getInt(LEGACY_RECIPE))
                : 0;
    }

    private static int maxAttempts(CompoundTag attempts) {
        int maximum = 0;
        for (String key : attempts.getAllKeys()) {
            if (attempts.contains(key, 3)) {
                maximum = Math.max(maximum, nonNegative(attempts.getInt(key)));
            }
        }
        return maximum;
    }

    private static int nonNegative(int attempts) {
        if (attempts < 0) {
            LOGGER.warn("Ignoring negative {}.{} value in item custom data", NAMESPACE, ATTEMPTS);
            return 0;
        }
        return attempts;
    }

    public static boolean hasAttempts(ItemStack stack) {
        return readAttempts(stack) > 0;
    }

    public static boolean hasAttempts(CompoundTag root) {
        return readAttempts(root) > 0;
    }

    public static ItemStack withFailure(ItemStack stack) {
        int attempts = readAttempts(stack);
        if (attempts < Integer.MAX_VALUE) {
            attempts++;
        }
        final int nextAttempts = attempts;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            withFailure(root, nextAttempts);
        });
        return stack;
    }

    public static ItemStack withFailure(ItemStack stack, ResourceLocation recipeId) {
        int attempts = readAttempts(stack, recipeId);
        if (attempts < Integer.MAX_VALUE) {
            attempts++;
        }
        int nextAttempts = attempts;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> withFailure(root, recipeId, nextAttempts));
        return stack;
    }

    public static CompoundTag withFailure(CompoundTag root) {
        int attempts = readAttempts(root);
        withFailure(root, attempts == Integer.MAX_VALUE ? Integer.MAX_VALUE : attempts + 1);
        return root;
    }

    public static CompoundTag withFailure(CompoundTag root, ResourceLocation recipeId) {
        int attempts = readAttempts(root, recipeId);
        withFailure(root, recipeId, attempts == Integer.MAX_VALUE ? Integer.MAX_VALUE : attempts + 1);
        return root;
    }

    /** Copies only this mod's failure history into a replacement ItemStack/tag. */
    public static ItemStack copyModData(ItemStack source, ItemStack target) {
        CustomData.update(DataComponents.CUSTOM_DATA, target, root -> copyModData(customData(source), root));
        return target;
    }

    /** Copies only this mod's namespace, preserving unrelated custom data on the target. */
    public static CompoundTag copyModData(CompoundTag source, CompoundTag target) {
        if (source.contains(NAMESPACE, 10)) {
            target.put(NAMESPACE, source.getCompound(NAMESPACE).copy());
        }
        return target;
    }

    private static void withFailure(CompoundTag root, int attempts) {
        CompoundTag modData = root.contains(NAMESPACE, 10) ? root.getCompound(NAMESPACE) : new CompoundTag();
        CompoundTag histories = modData.contains(ATTEMPTS, 10) ? modData.getCompound(ATTEMPTS) : new CompoundTag();
        histories.putInt(LEGACY_RECIPE, attempts);
        modData.put(ATTEMPTS, histories);
        root.put(NAMESPACE, modData);
    }

    private static void withFailure(CompoundTag root, ResourceLocation recipeId, int attempts) {
        CompoundTag modData = root.contains(NAMESPACE, 10) ? root.getCompound(NAMESPACE) : new CompoundTag();
        CompoundTag histories = modData.contains(ATTEMPTS, 10) ? modData.getCompound(ATTEMPTS) : new CompoundTag();
        histories.putInt(recipeId.toString(), attempts);
        modData.put(ATTEMPTS, histories);
        root.put(NAMESPACE, modData);
    }

    public static ItemStack clear(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            clear(root);
        });
        return stack;
    }

    public static ItemStack clear(ItemStack stack, ResourceLocation recipeId) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> clear(root, recipeId));
        return stack;
    }

    public static CompoundTag clear(CompoundTag root) {
        if (!root.contains(NAMESPACE, 10)) {
            return root;
        }
        CompoundTag modData = root.getCompound(NAMESPACE);
        modData.remove(ATTEMPTS);
        if (modData.isEmpty()) {
            root.remove(NAMESPACE);
        } else {
            root.put(NAMESPACE, modData);
        }
        return root;
    }

    public static CompoundTag clear(CompoundTag root, ResourceLocation recipeId) {
        if (!root.contains(NAMESPACE, 10)) {
            return root;
        }
        CompoundTag modData = root.getCompound(NAMESPACE);
        if (modData.contains(ATTEMPTS, 3)) {
            modData.remove(ATTEMPTS);
        } else if (modData.contains(ATTEMPTS, 10)) {
            CompoundTag histories = modData.getCompound(ATTEMPTS);
            histories.remove(recipeId.toString());
            if (histories.isEmpty()) {
                modData.remove(ATTEMPTS);
            } else {
                modData.put(ATTEMPTS, histories);
            }
        }
        if (modData.isEmpty()) {
            root.remove(NAMESPACE);
        } else {
            root.put(NAMESPACE, modData);
        }
        return root;
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
    }
}
