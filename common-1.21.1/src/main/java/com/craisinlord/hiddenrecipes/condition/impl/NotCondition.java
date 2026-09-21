package com.craisinlord.hiddenrecipes.condition.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeCondition;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeConditionType;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeConditions;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeTriggerType;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

public record NotCondition(HiddenRecipeCondition value) implements HiddenRecipeCondition {

    /** See {@code AndCondition.CODEC}'s javadoc — same circular-static-init hazard, same fix. */
    public static final MapCodec<NotCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.lazyInitialized(() -> HiddenRecipeConditions.CODEC).fieldOf("value").forGetter(NotCondition::value)
    ).apply(instance, NotCondition::new));

    @Override
    public boolean test(ServerPlayer player) {
        return !value.test(player);
    }

    @Override
    public HiddenRecipeConditionType<?> type() {
        return HiddenRecipeConditions.NOT;
    }

    @Override
    public Set<HiddenRecipeTriggerType> relevantTriggers() {
        return value.relevantTriggers();
    }
}
