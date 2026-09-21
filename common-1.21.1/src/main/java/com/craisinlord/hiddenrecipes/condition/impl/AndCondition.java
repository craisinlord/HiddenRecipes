package com.craisinlord.hiddenrecipes.condition.impl;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeCondition;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeConditionType;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeConditions;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeTriggerType;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record AndCondition(List<HiddenRecipeCondition> values) implements HiddenRecipeCondition {

    public static final MapCodec<AndCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        HiddenRecipeConditions.CODEC.listOf().fieldOf("values").forGetter(AndCondition::values)
    ).apply(instance, AndCondition::new));

    @Override
    public boolean test(ServerPlayer player) {
        for (HiddenRecipeCondition value : values) {
            if (!value.test(player)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public HiddenRecipeConditionType<?> type() {
        return HiddenRecipeConditions.AND;
    }

    @Override
    public Set<HiddenRecipeTriggerType> relevantTriggers() {
        Set<HiddenRecipeTriggerType> union = EnumSet.noneOf(HiddenRecipeTriggerType.class);
        values.forEach(value -> union.addAll(value.relevantTriggers()));
        return union;
    }

    @Override
    public List<HiddenRecipeCondition> leaves() {
        List<HiddenRecipeCondition> leaves = new ArrayList<>();
        values.forEach(value -> leaves.addAll(value.leaves()));
        return leaves;
    }
}
