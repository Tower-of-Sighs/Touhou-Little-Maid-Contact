package com.sighs.touhou_little_maid_epistalove;

import com.mafuyu404.oelib.forge.data.DataRegistry;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_epistalove.command.MaidLetterCommand;
import com.sighs.touhou_little_maid_epistalove.config.Config;
import com.sighs.touhou_little_maid_epistalove.data.LetterRuleRegistry;
import com.sighs.touhou_little_maid_epistalove.data.MaidLetterRule;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;

@Mod(TLMEpistalove.MODID)
public class TLMEpistalove {
    public static final String MODID = "touhou_little_maid_epistalove";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TLMEpistalove() {
        Config.register();

        DataRegistry.register(MaidLetterRule.class);

        LetterRuleRegistry.init();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MaidLetterCommand.register(event.getDispatcher());
    }
}
