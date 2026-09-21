package com.craisinlord.hiddenrecipes.condition;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/**
 * A single testable requirement for unlocking a hidden recipe. Implementations are
 * data-driven (registered by {@link HiddenRecipeConditionType} + a {@link MapCodec}) so
 * pack authors can compose new unlock rules purely in JSON.
 */
public interface HiddenRecipeCondition {

    boolean test(ServerPlayer player);

    HiddenRecipeConditionType<?> type();

    /**
     * Which player-state changes could flip this condition from false to true. Used to
     * index entries so only relevant events re-test them — see
     * {@link HiddenRecipeTriggerType}. Leaf conditions should return the precise trigger(s)
     * they care about; composites (and/or/not) return the union of their children's.
     */
    Set<HiddenRecipeTriggerType> relevantTriggers();

    /**
     * Flattens this condition tree down to its independently-testable "requirements" — used
     * by the JEI hint category to show "X/Y requirements met" progress, by calling
     * {@link #test} on each returned element directly.
     *
     * <p>The default (used by leaf conditions like {@code has_item}/{@code has_advancement},
     * and deliberately also by {@link com.craisinlord.hiddenrecipes.condition.impl.NotCondition})
     * is a singleton list of {@code this} — i.e. {@code not(x)} is reported as ONE
     * requirement whose met/unmet state is {@code not(x)}'s own correctly-negated
     * {@link #test} result, not {@code x}'s raw, un-negated one. This matters: flattening
     * straight through to {@code x} would make the progress display say a requirement is
     * "met" exactly when the player has the thing {@code not} requires them NOT to have —
     * backwards from what the pack author's condition actually means. Only {@code and}/
     * {@code or} override this, to concatenate their children's leaves — {@code not} always
     * has exactly one child anyway, so treating it as its own opaque leaf loses no useful
     * granularity while keeping every reported requirement's "met" state meaningful on its
     * own.
     */
    default List<HiddenRecipeCondition> leaves() {
        return List.of(this);
    }
}
