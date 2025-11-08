package com.sighs.touhou_little_maid_epistalove.config;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.screens.Screen;

public final class EpistaloveClothConfigScreen {
    private EpistaloveClothConfigScreen() {
    }

    public static Screen create(Screen parent) {
        return AutoConfig.getConfigScreen(ModConfig.class, parent).get();
    }
}