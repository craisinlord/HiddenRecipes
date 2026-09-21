package com.craisinlord.hiddenrecipes.condition.impl;

import com.mojang.serialization.Codec;
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

    /**
     * {@code Codec.lazyInitialized} defers the {@code HiddenRecipeConditions.CODEC} field
     * read until this codec is first actually used (i.e. until a hidden recipe is parsed),
     * not until this class is loaded. Without it, whichever of {@code HiddenRecipeConditions}
     * / {@code AndCondition} the JVM happens to touch first wins the class-init race and the
     * other reads the not-yet-assigned (still {@code null}) field of the one that's mid-init
     * — since both classes' static initializers reference each other. That silently
     * registered {@code "and"} (and {@code "or"}/{@code "not"}, same issue) with a null
     * {@code MapCodec}, which blew up as a bare NPE ({@code MapDecoder.decode(...) because
     * "elementDecoder" is null}) the first time an {@code and}-typed condition was parsed,
     * instead of a normal codec error.
     */
    public static final MapCodec<AndCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.lazyInitialized(() -> HiddenRecipeConditions.CODEC).listOf().fieldOf("values").forGetter(AndCondition::values)
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
