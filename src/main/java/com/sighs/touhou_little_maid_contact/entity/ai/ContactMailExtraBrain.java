package com.sighs.touhou_little_maid_contact.entity.ai;

import com.github.tartaricacid.touhoulittlemaid.api.entity.ai.IExtraMaidBrain;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.datafixers.util.Pair;
import com.sighs.touhou_little_maid_contact.entity.ai.behavior.LetterDeliveryBehavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;

import java.util.List;

public final class ContactMailExtraBrain implements IExtraMaidBrain {

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getIdleBehaviors() {
        return List.of(Pair.of(2, new LetterDeliveryBehavior()));
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getWorkBehaviors() {
        return List.of(Pair.of(2, new LetterDeliveryBehavior()));
    }
}