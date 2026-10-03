package com.pycoder.createprobabilitytuning.client;

import com.pycoder.createprobabilitytuning.config.ConfigDiagnostic;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

public final class WarningScreen extends Screen {
    private final ConfigDiagnostic diagnostic;
    private final Runnable onContinue;
    private final Screen parent;

    public WarningScreen(ConfigDiagnostic diagnostic, Runnable onContinue, Screen parent) {
        super(Component.literal("Create Probability Tuning"));
        this.diagnostic = diagnostic;
        this.onContinue = onContinue;
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Ignore warning / 无视警告"), button -> {
            onContinue.run();
            if (minecraft != null && minecraft.screen == this) {
                minecraft.setScreen(parent);
            }
        }).bounds(width / 2 - 125, height - 45, 120, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Edit config / 修改配置"), button ->
                ConfigDirectoryOpener.open(FMLPaths.CONFIGDIR.get()))
                .bounds(width / 2 + 5, height - 45, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen.render 会先于控件绘制背景。若最后调用它，
        // 模糊/菜单层会重新绘制到诊断文本上方。
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, Component.literal("Configuration warning / 配置警告"), width / 2, 35, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal(diagnostic.recipeId()), width / 2, 65, 0xFFCC55);
        graphics.drawString(font, Component.literal(diagnostic.message()), 30, 100, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.literal("建议修改配置文件 create_probability_tuning.json，以免游戏崩溃"),
                width / 2, height - 80, 0xFFCC55);
    }
}
