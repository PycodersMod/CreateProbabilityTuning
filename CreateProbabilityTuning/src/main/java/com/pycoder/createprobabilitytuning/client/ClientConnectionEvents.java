package com.pycoder.createprobabilitytuning.client;

import com.pycoder.createprobabilitytuning.network.ClientRuleSync;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = com.pycoder.createprobabilitytuning.CreateProbabilityTuning.MOD_ID, value = Dist.CLIENT)
public final class ClientConnectionEvents {
    private ClientConnectionEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientRuleSync.clear();
        ClientWarningHandler.reset();
    }
}
