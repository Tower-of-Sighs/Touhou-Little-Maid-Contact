package com.sighs.touhou_little_maid_epistalove;

import com.mafuyu404.oelib.fabric.data.DataRegistry;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_epistalove.command.MaidLetterCommand;
import com.sighs.touhou_little_maid_epistalove.config.ModConfig;
import com.sighs.touhou_little_maid_epistalove.data.LetterRuleRegistry;
import com.sighs.touhou_little_maid_epistalove.data.MaidLetterRule;
import com.sighs.touhou_little_maid_epistalove.event.ModEventHandler;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;

public class TLMEpistalove implements ModInitializer {
    public static final String MODID = "touhou_little_maid_epistalove";
    private static final Logger LOGGER = LogUtils.getLogger();


    @Override
    public void onInitialize() {
        ModConfig.init();
        ModEventHandler.init();
        DataRegistry.register(MaidLetterRule.class);
        LetterRuleRegistry.init();
        MaidLetterCommand.register();
    }
}
