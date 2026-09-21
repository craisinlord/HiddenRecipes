package com.craisinlord.hiddenrecipes.condition.impl;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeCondition;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeConditionType;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeConditions;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeTriggerType;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

public record HasAdvancementCondition(ResourceLocation advancement) implements HiddenRecipeCondition {

    public static final MapCodec<HasAdvancementCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("advancement").forGetter(HasAdvancementCondition::advancement)
    ).apply(instance, HasAdvancementCondition::new));

    @Override
    public boolean test(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        AdvancementHolder holder = server.getAdvancements().get(advancement);
        if (holder == null) {
            return false;
        }
        PlayerAdvancements progress = player.getAdvancements();
        return progress.getOrStartProgress(holder).isDone();
    }

    @Override
    public HiddenRecipeConditionType<?> type() {
        return HiddenRecipeConditions.HAS_ADVANCEMENT;
    }

    @Override
    public Set<HiddenRecipeTriggerType> relevantTriggers() {
        return Set.of(HiddenRecipeTriggerType.ADVANCEMENT);
    }
}
