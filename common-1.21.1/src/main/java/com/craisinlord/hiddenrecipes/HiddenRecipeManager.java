package com.craisinlord.hiddenrecipes;

import com.craisinlord.hiddenrecipes.condition.HiddenRecipeTriggerType;
import com.craisinlord.hiddenrecipes.data.HiddenRecipeEntry;
import com.craisinlord.hiddenrecipes.data.HiddenRecipePlayerDataStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side source of truth: which recipes are hidden at all, and which of those each
 * online player has unlocked so far.
 *
 * Modeled on how vanilla's advancement criteria triggers work rather than on blind
 * tick-polling: entries are indexed by the {@link HiddenRecipeTriggerType}s their
 * condition tree declares, so {@link #retest} only re-checks the entries that could
 * plausibly have just become true instead of every hidden recipe for every player.
 *
 * <p>Stale-TODO cleanup (found auditing this mod — the sync work this comment used to describe
 * as outstanding was actually completed a pass ago and just never had this comment updated):
 * whenever {@link #retest} unlocks something new, the updated set IS pushed to that player's
 * client already — see {@code HiddenRecipeEvents#notify}, which calls
 * {@code PlatformHelper#sendHiddenRecipesSync} right after a successful retest. Client-side JEI
 * (this mod ships its own plugin; no REI/EMI support exists) picks the change up via
 * {@code ClientHiddenRecipeState}'s change-listener mechanism.
 */
public final class HiddenRecipeManager {

    public static final HiddenRecipeManager INSTANCE = new HiddenRecipeManager();

    private Map<ResourceLocation, HiddenRecipeEntry> entriesByRecipe = Map.of();
    private Map<HiddenRecipeTriggerType, List<HiddenRecipeEntry>> entriesByTrigger = Map.of();
    private final Map<UUID, Set<ResourceLocation>> unlockedByPlayer = new HashMap<>();

    private HiddenRecipeManager() {
    }

    public void reload(Map<ResourceLocation, HiddenRecipeEntry> entries) {
        this.entriesByRecipe = Map.copyOf(entries);

        Map<HiddenRecipeTriggerType, List<HiddenRecipeEntry>> byTrigger = new EnumMap<>(HiddenRecipeTriggerType.class);
        for (HiddenRecipeTriggerType triggerType : HiddenRecipeTriggerType.values()) {
            byTrigger.put(triggerType, new ArrayList<>());
        }
        for (HiddenRecipeEntry entry : entries.values()) {
            Set<HiddenRecipeTriggerType> triggers = entry.condition().relevantTriggers();
            Set<HiddenRecipeTriggerType> effective = triggers.isEmpty()
                ? Set.of(HiddenRecipeTriggerType.GENERIC)
                : triggers;
            for (HiddenRecipeTriggerType triggerType : effective) {
                byTrigger.get(triggerType).add(entry);
            }
        }
        this.entriesByTrigger = Map.copyOf(byTrigger);
    }

    public boolean isHidden(ResourceLocation recipeId) {
        return entriesByRecipe.containsKey(recipeId);
    }

    public HiddenRecipeEntry get(ResourceLocation recipeId) {
        return entriesByRecipe.get(recipeId);
    }

    public Map<ResourceLocation, HiddenRecipeEntry> all() {
        return entriesByRecipe;
    }

    public boolean isLocked(ResourceLocation recipeId, ServerPlayer player) {
        if (!entriesByRecipe.containsKey(recipeId)) {
            return false;
        }
        return !unlockedFor(player).contains(recipeId);
    }

    /**
     * Re-tests only the hidden recipes indexed under the given trigger type(s) for this
     * player. Call this from the specific event that just happened (inventory changed,
     * advancement granted, ...) rather than on a blanket timer.
     */
    public Set<ResourceLocation> retest(ServerPlayer player, HiddenRecipeTriggerType... triggerTypes) {
        Set<ResourceLocation> unlocked = unlockedFor(player);
        Set<ResourceLocation> newlyUnlocked = new HashSet<>();
        for (HiddenRecipeTriggerType triggerType : triggerTypes) {
            for (HiddenRecipeEntry entry : entriesByTrigger.getOrDefault(triggerType, List.of())) {
                if (unlocked.contains(entry.recipe())) {
                    continue;
                }
                if (entry.condition().test(player)) {
                    unlocked.add(entry.recipe());
                    newlyUnlocked.add(entry.recipe());
                }
            }
        }
        return newlyUnlocked;
    }

    /**
     * Force-unlocks one recipe for one player, bypassing its condition entirely — backs
     * {@code /hiddenrecipes unlock}. Returns whether this actually changed anything ({@code
     * false} if the player already had it unlocked), so the caller (see
     * {@code HiddenRecipeCommands}) can decide whether to run the same post-unlock
     * side-effects (recipe-book grant, client sync, toast) that a real condition-triggered
     * unlock gets via {@code HiddenRecipeEvents#notify} — this method only touches the
     * in-memory bookkeeping and deliberately does not call those itself, to keep this class
     * free of a dependency on {@code HiddenRecipeEvents}/{@code PlatformHelper}.
     *
     * <p>Found and fixed auditing this mod: previously this method existed but nothing ever
     * called {@code notify} after it, so {@code /hiddenrecipes unlock} silently updated this
     * mod's own bookkeeping only — the manual-crafting mixins would correctly stop blocking
     * the recipe, but the player's client was never told (JEI kept showing the hint page, not
     * the real recipe, until their next join), the vanilla recipe book never got the entry,
     * and no toast fired. That defeated the command's whole stated purpose of letting an op
     * test a recipe without grinding out the real condition. See
     * {@code HiddenRecipeCommands}'s {@code unlock} handler for the fix.
     */
    public boolean forceUnlock(ServerPlayer player, ResourceLocation recipeId) {
        return unlockedFor(player).add(recipeId);
    }

    public Set<ResourceLocation> unlockedFor(ServerPlayer player) {
        return unlockedByPlayer.computeIfAbsent(player.getUUID(), id -> new HashSet<>());
    }

    /** Call when a player joins: loads their unlocked set from disk into the in-memory cache. */
    public void loadPlayer(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        unlockedByPlayer.put(player.getUUID(), HiddenRecipePlayerDataStorage.load(server, player.getUUID()));
    }

    /** Call when a player leaves: flushes their unlocked set to disk and evicts the cache entry. */
    public void unloadPlayer(ServerPlayer player) {
        flushPlayer(player);
        unlockedByPlayer.remove(player.getUUID());
    }

    /** Writes this player's current unlocked set to disk without evicting the in-memory cache. */
    public void flushPlayer(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        Set<ResourceLocation> unlocked = unlockedByPlayer.get(player.getUUID());
        if (server != null && unlocked != null) {
            HiddenRecipePlayerDataStorage.save(server, player.getUUID(), unlocked);
        }
    }

    /** Call on server shutdown: flushes every currently-online player so a clean stop never loses unlocks. */
    public void flushAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            flushPlayer(player);
        }
    }
}
