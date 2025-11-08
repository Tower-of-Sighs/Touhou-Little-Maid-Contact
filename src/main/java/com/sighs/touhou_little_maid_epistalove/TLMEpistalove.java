package com.sighs.touhou_little_maid_epistalove;

import com.mafuyu404.oelib.neoforge.data.DataRegistry;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_epistalove.command.MaidLetterCommand;
import com.sighs.touhou_little_maid_epistalove.component.TLMContactDataComponents;
import com.sighs.touhou_little_maid_epistalove.config.Config;
import com.sighs.touhou_little_maid_epistalove.data.LetterRuleRegistry;
import com.sighs.touhou_little_maid_epistalove.data.MaidLetterRule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(TLMEpistalove.MODID)
public class TLMEpistalove {
    public static final String MODID = "touhou_little_maid_epistalove";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TLMEpistalove(IEventBus modEventBus, ModContainer modContainer) {
        Config.register(modContainer);

        TLMContactDataComponents.DATA_COMPONENTS.register();

        DataRegistry.register(MaidLetterRule.class);

        LetterRuleRegistry.init();

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        if (!FMLLoader.isProduction()) {
            MaidLetterCommand.register(event.getDispatcher());
        }
    }
}
