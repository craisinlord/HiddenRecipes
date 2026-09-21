package com.craisinlord.hiddenrecipes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Tracks, per furnace/blast-furnace/smoker {@link Container} (the block entity itself,
 * which implements {@code Container}), which {@link ServerPlayer} last manually opened it —
 * the "whoever last opened/inserted via the menu" default this project decided on for
 * attributing a smelt to a player, since {@link com.craisinlord.hiddenrecipes.condition.HiddenRecipeCondition#test}
 * requires a {@code ServerPlayer} and smelting itself (unlike crafting-grid or smithing) can
 * happen with no player present at all.
 *
 * <p>Populated by {@code AbstractFurnaceBlockMixin} (records the player on manual open) and
 * read by {@code AbstractFurnaceBlockEntityMixin} (checks the recorded player when deciding
 * whether to block a tick's smelt). Deliberately NOT populated by hopper insertion — a
 * furnace that's only ever been fed by automation has no entry here, and enforcement is
 * skipped for it. That's a known, accepted limitation (documented in README.md), not a bug:
 * there is no player to test the unlock condition against in that case.
 *
 * <p>Pure bookkeeping, no vanilla-internals guessing involved — the two mixins that populate
 * and read this are where the actual verification risk lives.
 */
final class FurnaceInteractionTracker {

    // Weak keys so a removed/unloaded block entity's entry can be collected; the ServerPlayer
    // value is already retained for the player's whole session by the server's own player
    // list, so a strong reference here adds no real leak risk.
    private static final Map<Container, ServerPlayer> LAST_OPENER = new WeakHashMap<>();

    static synchronized void recordOpener(Container container, ServerPlayer player) {
        LAST_OPENER.put(container, player);
    }

    static synchronized ServerPlayer lastOpener(Container container) {
        return LAST_OPENER.get(container);
    }

    private FurnaceInteractionTracker() {
    }
}
