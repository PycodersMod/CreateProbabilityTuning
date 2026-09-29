package com.pycoder.createprobabilitytuning.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessDataTest {
    private static final ResourceLocation PRESSING = ResourceLocation.parse("create:pressing/iron_ingot");
    private static final ResourceLocation SEQUENCED = ResourceLocation.parse("create:sequenced_assembly/precision_mechanism");
    @Test
    void emptyStackHasNoAttempts() {
        CompoundTag tag = new CompoundTag();
        assertFalse(ProcessData.hasAttempts(tag));
        assertEquals(0, ProcessData.readAttempts(tag));
    }

    @Test
    void failuresAccumulateFromOne() {
        CompoundTag tag = new CompoundTag();
        ProcessData.withFailure(tag);
        ProcessData.withFailure(tag);
        assertTrue(ProcessData.hasAttempts(tag));
        assertEquals(2, ProcessData.readAttempts(tag));
    }

    @Test
    void successClearsOnlyThisModData() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Unrelated", 7);
        tag.putInt(ProcessData.ATTEMPTS, 3);
        CompoundTag root = new CompoundTag();
        root.put(ProcessData.NAMESPACE, tag);

        ProcessData.clear(root);

        assertFalse(ProcessData.hasAttempts(root));
        assertEquals(7, root.getCompound(ProcessData.NAMESPACE).getInt("Unrelated"));
    }

    @Test
    void historiesAreStoredPerRecipe() {
        CompoundTag root = new CompoundTag();

        ProcessData.withFailure(root, PRESSING);
        ProcessData.withFailure(root, PRESSING);
        ProcessData.withFailure(root, SEQUENCED);

        assertEquals(2, ProcessData.readAttempts(root, PRESSING));
        assertEquals(1, ProcessData.readAttempts(root, SEQUENCED));
        assertEquals(0, ProcessData.readAttempts(root, ResourceLocation.parse("create:mixing/brass_ingot")));

        ProcessData.clear(root, PRESSING);
        assertEquals(0, ProcessData.readAttempts(root, PRESSING));
        assertEquals(1, ProcessData.readAttempts(root, SEQUENCED));
    }

    @Test
    void copiesFailureHistoryToCreateTransitionalItem() {
        CompoundTag source = new CompoundTag();
        ProcessData.withFailure(source, PRESSING);
        ProcessData.withFailure(source, SEQUENCED);

        CompoundTag target = new CompoundTag();
        target.putInt("Unrelated", 7);
        ProcessData.copyModData(source, target);

        assertEquals(1, ProcessData.readAttempts(target, PRESSING));
        assertEquals(1, ProcessData.readAttempts(target, SEQUENCED));
        assertEquals(7, target.getInt("Unrelated"));
    }

    @Test
    void genericHistoryQuerySeesRecipeKeyedAttempts() {
        CompoundTag root = new CompoundTag();

        ProcessData.withFailure(root, PRESSING);

        assertTrue(ProcessData.hasAttempts(root));
        assertEquals(1, ProcessData.readAttempts(root));
    }

    @Test
    void legacyAttemptsCanBeReadForSpecificRecipe() {
        CompoundTag root = new CompoundTag();
        ProcessData.withFailure(root);

        assertEquals(1, ProcessData.readAttempts(root, PRESSING));
    }
}
