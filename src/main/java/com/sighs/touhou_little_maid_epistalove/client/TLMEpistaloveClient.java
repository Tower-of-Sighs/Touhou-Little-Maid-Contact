package com.sighs.touhou_little_maid_epistalove.client;

import com.github.tartaricacid.touhoulittlemaid.init.registry.CompatRegistry;
import com.sighs.touhou_little_maid_epistalove.TLMEpistalove;
import com.sighs.touhou_little_maid_epistalove.config.EpistaloveClothConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = TLMEpistalove.MODID, dist = Dist.CLIENT)
public class TLMEpistaloveClient {

    public TLMEpistaloveClient(IEventBus modEventBus, ModContainer modContainer) {
        this.registerConfigMenu(modContainer);
    }

    private void registerConfigMenu(ModContainer modContainer) {
        ModFileInfo clothConfigInfo = LoadingModList.get().getModFileById(CompatRegistry.CLOTH_CONFIG);
        if (clothConfigInfo != null) {
            EpistaloveClothConfigScreen.registerModsPage(modContainer);
        } else {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }
}
