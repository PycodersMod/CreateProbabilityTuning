package com.pycoder.createprobabilitytuning.network;

import com.pycoder.createprobabilitytuning.CreateProbabilityTuning;
import com.pycoder.createprobabilitytuning.config.RecipeRule;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record RuleSyncPayload(long revision, List<Entry> entries) implements CustomPacketPayload {
    public static final Type<RuleSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateProbabilityTuning.MOD_ID, "rule_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuleSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RuleSyncPayload decode(RegistryFriendlyByteBuf buffer) {
            long revision = buffer.readLong();
            int count = buffer.readVarInt();
            if (count < 0 || count > 4096) {
                throw new IllegalArgumentException("Invalid probability rule count: " + count);
            }
            List<Entry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String id = buffer.readUtf(256);
                boolean enabled = buffer.readBoolean();
                double initial = buffer.readDouble();
                RecipeRule.Mode mode = buffer.readEnum(RecipeRule.Mode.class);
                int changes = buffer.readVarInt();
                if (changes < 0 || changes > 256) throw new IllegalArgumentException("Invalid change count");
                List<Double> values = new ArrayList<>(changes);
                for (int j = 0; j < changes; j++) values.add(buffer.readDouble());
                String formula = buffer.readUtf(1024);
                int maxN = buffer.readVarInt();
                if (maxN < 0) throw new IllegalArgumentException("Invalid max_n");
                entries.add(new Entry(id, new RecipeRule(enabled, initial, mode, values, formula, maxN)));
            }
            return new RuleSyncPayload(revision, entries);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RuleSyncPayload payload) {
            if (payload.entries.size() > 4096) throw new IllegalArgumentException("Too many probability rules");
            buffer.writeLong(payload.revision);
            buffer.writeVarInt(payload.entries.size());
            for (Entry entry : payload.entries) {
                buffer.writeUtf(entry.recipeId, 256);
                RecipeRule rule = entry.rule;
                buffer.writeBoolean(rule.enabled());
                buffer.writeDouble(rule.initial());
                buffer.writeEnum(rule.mode());
                buffer.writeVarInt(rule.changes().size());
                rule.changes().forEach(buffer::writeDouble);
                buffer.writeUtf(rule.formula(), 1024);
                buffer.writeVarInt(rule.maxN());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(String recipeId, RecipeRule rule) {
    }
}
