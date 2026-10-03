package com.pycoder.createprobabilitytuning.recipe;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ProcessingDecision(Status status, ItemStack input, List<ItemStack> outputs, boolean returnsInput) {
    public enum Status { DISABLED, UNAVAILABLE, SUCCESS, FAILURE }

    public ProcessingDecision(Status status, ItemStack input, List<ItemStack> outputs) {
        this(status, input, outputs, status == Status.FAILURE && !outputs.isEmpty());
    }

    public ProcessingDecision {
        outputs = List.copyOf(outputs);
    }
}
