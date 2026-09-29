package com.pycoder.createprobabilitytuning.network;

import com.pycoder.createprobabilitytuning.config.ModConfig;
import com.pycoder.createprobabilitytuning.config.RecipeRule;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class RuleSyncNetwork {
    private static final AtomicLong REVISION = new AtomicLong();
    private static volatile ModConfig current = new ModConfig(Map.of(), List.of());

    private RuleSyncNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(RuleSyncPayload.TYPE, RuleSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientRuleSync.accept(payload)));
    }

    public static void replace(ModConfig config) {
        current = config;
        REVISION.incrementAndGet();
    }

    public static void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, snapshot());
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendTo(player);
    }

    private static RuleSyncPayload snapshot() {
        List<RuleSyncPayload.Entry> entries = new ArrayList<>();
        current.recipes().forEach((id, rule) -> entries.add(new RuleSyncPayload.Entry(id, rule)));
        return new RuleSyncPayload(REVISION.get(), entries);
    }
}
