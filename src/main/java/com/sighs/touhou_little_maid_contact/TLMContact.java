package com.sighs.touhou_little_maid_contact;

import com.mafuyu404.oelib.forge.data.DataRegistry;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.command.MaidLetterCommand;
import com.sighs.touhou_little_maid_contact.data.MaidLetterRegistry;
import com.sighs.touhou_little_maid_contact.data.MaidLetterRule;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TLMContact.MODID)
public class TLMContact {
    public static final String MODID = "touhou_little_maid_contact";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TLMContact() {
        Config.register();
        DataRegistry.register(MaidLetterRule.class);
        MaidLetterRegistry.init();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MaidLetterCommand.register(event.getDispatcher());
    }
}
