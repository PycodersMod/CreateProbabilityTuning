package com.pycoder.createprobabilitytuning;

import com.mojang.logging.LogUtils;
import com.pycoder.createprobabilitytuning.config.ConfigManager;
import com.pycoder.createprobabilitytuning.config.ConfigRuntime;
import com.pycoder.createprobabilitytuning.config.ModConfig;
import com.pycoder.createprobabilitytuning.create.CreateHook;
import com.pycoder.createprobabilitytuning.recipe.RecipeIndex;
import com.pycoder.createprobabilitytuning.recipe.RecipeScanner;
import com.pycoder.createprobabilitytuning.nbt.PlacedProcessEvents;
import com.pycoder.createprobabilitytuning.network.RuleSyncNetwork;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

@Mod(CreateProbabilityTuning.MOD_ID)
public class CreateProbabilityTuning {
    public static final String MOD_ID = "createprobabilitytuning";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ConfigRuntime configRuntime;
    private final RecipeScanner recipeScanner = new RecipeScanner();
    private final CreateHook createHook = new CreateHook();

    public CreateProbabilityTuning(IEventBus modBus) {
        configRuntime = new ConfigRuntime(
                FMLPaths.CONFIGDIR.get().resolve("create_probability_tuning.json"),
                new ConfigManager());
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(PlacedProcessEvents::onPlace);
        NeoForge.EVENT_BUS.addListener(PlacedProcessEvents::onDrops);
        NeoForge.EVENT_BUS.addListener(PlacedProcessEvents::onPiston);
        NeoForge.EVENT_BUS.addListener(RuleSyncNetwork::onLogin);
        modBus.addListener(RuleSyncNetwork::register);
        modBus.addListener(this::onRegisterGameTests);
        LOGGER.info("{} loaded", MOD_ID);
    }

    private void onRegisterGameTests(RegisterGameTestsEvent event) {
        event.register(com.pycoder.createprobabilitytuning.gametest.ProbabilityTuningGameTests.class);
        LOGGER.info("Registered CreateProbabilityTuning GameTests");
    }

    private void onServerStarting(ServerStartingEvent event) {
        ModConfig config = configRuntime.reload();
        RuleSyncNetwork.replace(config);
        RecipeIndex recipeIndex = recipeScanner.scan(event.getServer().getRecipeManager(), config);
        createHook.replaceIndex(recipeIndex);
        event.getServer().getPlayerList().getPlayers().forEach(RuleSyncNetwork::sendTo);
        config.diagnostics().forEach(diagnostic -> {
            if (diagnostic.severity() == com.pycoder.createprobabilitytuning.config.ConfigDiagnostic.Severity.ERROR) {
                LOGGER.error("{}: {}", diagnostic.recipeId(), diagnostic.message());
            } else {
                LOGGER.warn("{}: {}", diagnostic.recipeId(), diagnostic.message());
            }
        });
        LOGGER.info("Loaded {} configured rule(s), {} matching recipe(s)", config.recipes().size(), recipeIndex.byId().size());
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier barrier,
                                                   net.minecraft.server.packs.resources.ResourceManager resourceManager,
                                                   net.minecraft.util.profiling.ProfilerFiller preparationsProfiler,
                                                   net.minecraft.util.profiling.ProfilerFiller reloadProfiler,
                                                   java.util.concurrent.Executor backgroundExecutor,
                                                   java.util.concurrent.Executor gameExecutor) {
                return barrier.wait(net.minecraft.util.Unit.INSTANCE).thenRunAsync(() -> {
                    LOGGER.debug("CreateProbabilityTuning reload listener entered");
                    ModConfig config = configRuntime.reload();
                    RuleSyncNetwork.replace(config);
                    LOGGER.debug("CreateProbabilityTuning config reloaded; scanning recipes");
                    RecipeIndex recipeIndex = recipeScanner.scan(event.getServerResources().getRecipeManager(), config);
                    createHook.replaceIndex(recipeIndex);
                    LOGGER.debug("CreateProbabilityTuning recipe scan finished with {} entries", recipeIndex.byId().size());
                }, gameExecutor);
            }
        });
    }
}
