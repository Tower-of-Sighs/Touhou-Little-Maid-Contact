package com.sighs.touhou_little_maid_epistalove.integration.kjs;

import com.sighs.touhou_little_maid_epistalove.data.LetterRuleRegistry;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;

public class KJSLetterPlugin implements KubeJSPlugin {
    public static final EventGroup LETTER_EVENTS = EventGroup.of("LetterEvents");

    public static final EventHandler REGISTER_LETTER_RULES = LETTER_EVENTS.server("registerLetterRules",
            () -> LetterRuleRegistrationEvent.class);

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(LETTER_EVENTS);
    }


    @Override
    public void registerBindings(BindingRegistry registry) {
        registry.add("LetterAPI", LetterAPI.class);
        registry.add("LetterGenerators", LetterGenerators.class);

        registry.add("LetterRuleBuilder", LetterRuleBuilder.class);
    }

    @Override
    public void afterScriptsLoaded(ScriptManager manager) {
        if (manager.scriptType == ScriptType.SERVER) {
            LetterRuleRegistry.clearDynamicRules();
            REGISTER_LETTER_RULES.post(new LetterRuleRegistrationEvent());
        }
    }
}