package com.pycoder.createprobabilitytuning.client;

import com.pycoder.createprobabilitytuning.config.ConfigDiagnostic;
import com.pycoder.createprobabilitytuning.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.List;

@EventBusSubscriber(modid = com.pycoder.createprobabilitytuning.CreateProbabilityTuning.MOD_ID, value = Dist.CLIENT)
public final class ClientWarningHandler {
    private static final WarningQueue WARNINGS = new WarningQueue();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean loaded;
    private static int ticksSinceLoad;

    private ClientWarningHandler() {
    }

    public static void enqueue(List<ConfigDiagnostic> diagnostics) {
        for (ConfigDiagnostic diagnostic : diagnostics.stream()
                .filter(ClientWarningHandler::shouldDisplay)
                .toList()) {
            WARNINGS.enqueue(List.of(diagnostic));
        }
    }

    public static void reset() {
        loaded = false;
        ticksSinceLoad = 0;
        WARNINGS.clear();
    }

    private static boolean shouldDisplay(ConfigDiagnostic diagnostic) {
        return diagnostic.type() == ConfigDiagnostic.Type.NUMERIC_RISK;
    }

    public static void showNext() {
        Minecraft minecraft = Minecraft.getInstance();
        ConfigDiagnostic diagnostic = WARNINGS.poll();
        if (diagnostic != null) {
            LOGGER.info("Displaying configuration warning screen for {}", diagnostic.recipeId());
            minecraft.setScreen(new WarningScreen(diagnostic, ClientWarningHandler::showNext, minecraft.screen));
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (loaded) {
            if (++ticksSinceLoad == 20) {
                showNext();
            }
            return;
        }
        // The first client ticks can happen before recipe/resource reload has
        // completed. Delay the screen until the initial reload settles.
        loaded = true;
        var configPath = FMLPaths.CONFIGDIR.get().resolve("create_probability_tuning.json");
        enqueue(new ConfigManager().load(configPath).diagnostics());
    }

}
