package com.pycoder.createprobabilitytuning.nbt;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.minecraft.core.BlockPos;

/** 在已放置方块状态与物品堆之间传递加工历史记录。 */
public final class PlacedProcessEvents {
    private PlacedProcessEvents() {
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        PlacedProcessData data = PlacedProcessData.get(level);
        if (event.isCanceled()) {
            return;
        }
        // 同一坐标上的普通方块替换会使旧快照失效；不能让仅按坐标索引的记录泄漏到新方块。
        data.remove(event.getPos());
        ItemStack placed = findPlacedStack(event);
        if (!placed.isEmpty() && ProcessData.hasAttempts(placed)) {
            data.put(event.getPos(), placed);
        }
    }

    public static void onDrops(BlockDropsEvent event) {
        ServerLevel level = event.getLevel();
        ItemStack stored = PlacedProcessData.get(level).get(event.getPos(), event.getState().getBlock().asItem());
        if (stored.isEmpty()) {
            return;
        }

        boolean replaced = false;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack current = drop.getItem();
            if (!replaced && current.getItem() == stored.getItem()) {
                drop.setItem(stored.copyWithCount(Math.max(1, current.getCount())));
                replaced = true;
            }
        }
        if (!replaced) {
            event.getDrops().add(new ItemEntity(level,
                    event.getPos().getX() + 0.5D,
                    event.getPos().getY() + 0.5D,
                    event.getPos().getZ() + 0.5D,
                    stored));
        }
        PlacedProcessData.get(level).remove(event.getPos());
    }

    public static void onPiston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.isCanceled() || event.getStructureHelper() == null) return;
        var resolver = event.getStructureHelper();
        PlacedProcessData data = PlacedProcessData.get(level);
        java.util.Map<BlockPos, ItemStack> moving = new java.util.HashMap<>();
        for (BlockPos from : resolver.getToPush()) {
            ItemStack stack = data.get(from, level.getBlockState(from).getBlock().asItem());
            if (!stack.isEmpty()) moving.put(from, stack);
        }
        moving.forEach((from, ignored) -> data.remove(from));
        moving.forEach((from, stack) -> data.put(from.relative(event.getDirection()), stack));
    }

    private static ItemStack findPlacedStack(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return ItemStack.EMPTY;
        }
        for (ItemStack candidate : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (!candidate.isEmpty()
                    && candidate.getItem() == event.getPlacedBlock().getBlock().asItem()
                    && ProcessData.hasAttempts(candidate)) {
                return candidate.copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }
}
