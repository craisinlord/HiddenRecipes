package com.craisinlord.hiddenrecipes.network;

import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Client-side cache of the local player's unlocked hidden recipes, kept in sync from
 * {@link HiddenRecipesSyncPayload}. Plain data holder with no client-only Minecraft
 * classes involved, so it's safe to reference from common code on the logical server too
 * (it will simply never receive updates there).
 *
 * Recipe-viewer plugins (JEI/REI/EMI) read {@link #isUnlocked} and register a
 * {@link #addChangeListener} so this class never needs to know they exist — keeps the
 * optional {@code src/jei/} source set from being referenced by code that has to run
 * whether or not the viewer mod is even installed.
 */
public final class ClientHiddenRecipeState {

    private static volatile Set<ResourceLocation> unlocked = Set.of();
    /**
     * Whether {@link #set} has been called at least once since the last {@link #clear()}.
     * Guards {@link #UNLOCK_LISTENERS}: the very first sync after (re)connecting is the
     * server handing us our existing save data, not something "just" unlocked, so it must
     * not fire unlock notifications (e.g. a toast) for every recipe the player already knew
     * about from a previous session.
     */
    private static volatile boolean receivedInitialSync = false;
    private static final List<Runnable> CHANGE_LISTENERS = new CopyOnWriteArrayList<>();
    /** Fired only with the recipes newly added by a given {@link #set} call — see {@link #receivedInitialSync}. */
    private static final List<Consumer<Set<ResourceLocation>>> UNLOCK_LISTENERS = new CopyOnWriteArrayList<>();

    public static void set(List<ResourceLocation> newUnlockedList) {
        Set<ResourceLocation> newUnlocked = Set.copyOf(newUnlockedList);
        Set<ResourceLocation> previous = unlocked;
        unlocked = newUnlocked;

        if (receivedInitialSync) {
            Set<ResourceLocation> newlyUnlocked = new HashSet<>(newUnlocked);
            newlyUnlocked.removeAll(previous);
            if (!newlyUnlocked.isEmpty()) {
                UNLOCK_LISTENERS.forEach(listener -> listener.accept(newlyUnlocked));
            }
        } else {
            receivedInitialSync = true;
        }

        CHANGE_LISTENERS.forEach(Runnable::run);
    }

    public static boolean isUnlocked(ResourceLocation recipeId) {
        return unlocked.contains(recipeId);
    }

    public static void clear() {
        unlocked = Set.of();
        receivedInitialSync = false;
        CHANGE_LISTENERS.forEach(Runnable::run);
    }

    /** Called after every {@link #set}/{@link #clear} — e.g. a JEI plugin re-hiding recipes to match. */
    public static void addChangeListener(Runnable listener) {
        CHANGE_LISTENERS.add(listener);
    }

    /**
     * Called only when {@link #set} adds recipes beyond what was already known (never on the
     * initial post-connect sync) — e.g. showing a player-facing unlock toast. Each loader's
     * client entrypoint registers its own listener here rather than this class rendering
     * anything itself, for the same reason {@link #addChangeListener} exists: this class stays
     * free of client-only Minecraft classes so it's safe on the logical server too.
     */
    public static void addUnlockListener(Consumer<Set<ResourceLocation>> listener) {
        UNLOCK_LISTENERS.add(listener);
    }

    private ClientHiddenRecipeState() {
    }
}
