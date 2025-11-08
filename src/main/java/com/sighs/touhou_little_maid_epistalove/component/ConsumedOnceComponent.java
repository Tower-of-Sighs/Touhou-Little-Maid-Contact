package com.sighs.touhou_little_maid_epistalove.component;

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.resources.ResourceLocation;

public interface ConsumedOnceComponent extends AutoSyncedComponent {
    void markConsumed(ResourceLocation triggerKey);

    boolean hasConsumed(ResourceLocation triggerKey);

    void clearConsumed(ResourceLocation triggerKey);
}
