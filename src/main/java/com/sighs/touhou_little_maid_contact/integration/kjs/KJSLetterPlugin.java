package com.sighs.touhou_little_maid_contact.integration.kjs;

import com.sighs.touhou_little_maid_contact.data.LetterRuleRegistry;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.BindingsEvent;

public class KJSLetterPlugin extends KubeJSPlugin {
    public static final EventGroup LETTER_EVENTS = EventGroup.of("LetterEvents");

    public static final EventHandler REGISTER_LETTER_RULES = LETTER_EVENTS.server("registerLetterRules",
            () -> LetterRuleRegistrationEvent.class);

    @Override
    public void registerEvents() {
        LETTER_EVENTS.register();
    }


    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("ContactLetterAPI", ContactLetterAPI.class);
        event.add("LetterGenerators", LetterGenerators.class);

        event.add("LetterRuleBuilder", LetterRuleBuilder.class);
    }

    @Override
    public void onServerReload() {
        LetterRuleRegistry.clearDynamicRules();
        REGISTER_LETTER_RULES.post(new LetterRuleRegistrationEvent());
    }
}