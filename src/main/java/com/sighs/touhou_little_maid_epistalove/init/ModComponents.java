package com.sighs.touhou_little_maid_epistalove.init;

import com.sighs.touhou_little_maid_epistalove.TLMEpistalove;
import com.sighs.touhou_little_maid_epistalove.component.ConsumedOnceComponent;
import com.sighs.touhou_little_maid_epistalove.component.ConsumedOnceComponentImpl;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import net.minecraft.resources.ResourceLocation;

public class ModComponents implements EntityComponentInitializer {
    public static final ComponentKey<ConsumedOnceComponent> CONSUMED_ONCE =
            ComponentRegistry.getOrCreate(new ResourceLocation(TLMEpistalove.MODID, "maidmail_consumed"), ConsumedOnceComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(CONSUMED_ONCE, player -> new ConsumedOnceComponentImpl(), RespawnCopyStrategy.ALWAYS_COPY);
    }
}