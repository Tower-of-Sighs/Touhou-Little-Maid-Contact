package com.sighs.touhou_little_maid_contact;

import com.mafuyu404.oelib.neoforge.data.DataRegistry;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.command.MaidLetterCommand;
import com.sighs.touhou_little_maid_contact.component.TLMContactDataComponents;
import com.sighs.touhou_little_maid_contact.config.Config;
import com.sighs.touhou_little_maid_contact.data.LetterRuleRegistry;
import com.sighs.touhou_little_maid_contact.data.MaidLetterRule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(TLMContact.MODID)
public class TLMContact {
    public static final String MODID = "touhou_little_maid_contact";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TLMContact(IEventBus modEventBus, ModContainer modContainer) {
        Config.register(modContainer);

        // 注册数据组件
        TLMContactDataComponents.DATA_COMPONENTS.register();

        DataRegistry.register(MaidLetterRule.class);

        LetterRuleRegistry.init();

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MaidLetterCommand.register(event.getDispatcher());
    }
}
