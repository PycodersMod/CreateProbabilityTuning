package com.pycoder.createprobabilitytuning.nbt;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/** 按维度持久化物品快照，供支持加工历史的已放置方块使用。 */
public final class PlacedProcessData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_ID = "create_probability_tuning_placed_process";
    private final Map<Long, Entry> entries = new HashMap<>();

    public static PlacedProcessData load(CompoundTag tag, HolderLookup.Provider registries) {
        PlacedProcessData data = new PlacedProcessData();
        CompoundTag values = tag.getCompound("Entries");
        for (String key : values.getAllKeys()) {
            try {
                CompoundTag encoded = values.getCompound(key);
                ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), encoded)
                        .resultOrPartial(LOGGER::error)
                        .ifPresent(stack -> {
                            ResourceLocation expected = encoded.contains("ExpectedItem", 8)
                                    ? ResourceLocation.tryParse(encoded.getString("ExpectedItem"))
                                    : BuiltInRegistries.ITEM.getKey(stack.getItem());
                            if (expected != null) data.entries.put(Long.parseLong(key), new Entry(stack, expected));
                        });
            } catch (RuntimeException exception) {
                LOGGER.warn("Ignoring invalid placed processing entry {}", key, exception);
            }
        }
        return data;
    }

    public static SavedData.Factory<PlacedProcessData> factory(HolderLookup.Provider registries) {
        return new SavedData.Factory<>(PlacedProcessData::new,
                (tag, provider) -> load(tag, provider));
    }

    public static PlacedProcessData get(net.minecraft.server.level.ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(level.registryAccess()), DATA_ID);
    }

    public ItemStack get(BlockPos pos, Item expectedItem) {
        Entry entry = entries.get(pos.asLong());
        if (entry == null || entry.expectedItem() != BuiltInRegistries.ITEM.getKey(expectedItem)) {
            return ItemStack.EMPTY;
        }
        return entry.stack().copy();
    }

    public void put(BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) remove(pos);
        else entries.put(pos.asLong(), new Entry(stack.copyWithCount(1), BuiltInRegistries.ITEM.getKey(stack.getItem())));
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (entries.remove(pos.asLong()) != null) setDirty();
    }

    public void move(BlockPos from, BlockPos to) {
        Entry entry = entries.remove(from.asLong());
        if (entry != null) entries.put(to.asLong(), entry);
        if (entry != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag values = new CompoundTag();
        for (Map.Entry<Long, Entry> entry : entries.entrySet()) {
            try {
                CompoundTag encoded = ItemStack.CODEC.encodeStart(
                                RegistryOps.create(NbtOps.INSTANCE, registries), entry.getValue().stack())
                        .result().filter(encodedTag -> encodedTag instanceof CompoundTag)
                        .map(encodedTag -> (CompoundTag) encodedTag)
                        .orElseGet(CompoundTag::new);
                encoded.putString("ExpectedItem", entry.getValue().expectedItem().toString());
                values.put(Long.toString(entry.getKey()), encoded);
            } catch (RuntimeException exception) {
                LOGGER.error("Unable to save placed processing entry {}", entry.getKey(), exception);
            }
        }
        tag.put("Entries", values);
        return tag;
    }

    private record Entry(ItemStack stack, ResourceLocation expectedItem) {
    }
}
