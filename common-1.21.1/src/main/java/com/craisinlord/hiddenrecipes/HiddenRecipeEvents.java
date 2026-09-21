package com.craisinlord.hiddenrecipes;

import com.craisinlord.hiddenrecipes.condition.HiddenRecipeTriggerType;
import com.craisinlord.hiddenrecipes.data.HiddenRecipeEntry;
import com.craisinlord.hiddenrecipes.network.HiddenRecipeDefinitionsSyncPayload;
import com.craisinlord.hiddenrecipes.platform.PlatformHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Loader-agnostic event handling. Each loader module calls into here from its own
 * connection/tick/advancement hooks (see {@code HiddenRecipesFabric} in fabric-1.21.1 and
 * {@code HiddenRecipeNeoForgeEvents} in neoforge-1.21.1) so the actual condition-checking
 * logic only has to be written once.
 */
public final class HiddenRecipeEvents {

    /** How often (in ticks) to cheaply check for inventory changes and poll cheap triggers. */
    private static final int CHECK_INTERVAL_TICKS = 10;
    /** How often (in ticks) to flush a player's unlocked set to disk, bounding crash data loss. ~5 minutes. */
    private static final int AUTOSAVE_INTERVAL_TICKS = 6000;

    private static final WeakHashMap<ServerPlayer, Integer> TICK_COUNTERS = new WeakHashMap<>();
    private static final WeakHashMap<ServerPlayer, Integer> LAST_INVENTORY_FINGERPRINT = new WeakHashMap<>();

    public static void onPlayerJoin(ServerPlayer player) {
        HiddenRecipeManager.INSTANCE.loadPlayer(player);
        PlatformHelper.getInstance().sendHiddenRecipesSync(player, HiddenRecipeManager.INSTANCE.unlockedFor(player));
        syncDefinitionsToPlayer(player);
        // Re-grants the vanilla recipe book for everything already unlocked in our own save
        // data (see grantRecipeBookEntries's javadoc for why this is needed on every join, not
        // just once) — idempotent if the player already has it, so safe to call unconditionally.
        grantRecipeBookEntries(player, HiddenRecipeManager.INSTANCE.unlockedFor(player));
    }

    /**
     * Sends every currently-defined hidden recipe's id + hint (not the unlock condition — see
     * {@link HiddenRecipeDefinitionsSyncPayload}'s javadoc for why) to one player. Called from
     * {@link #onPlayerJoin} for both loaders, and additionally per-player by NeoForge's
     * {@code OnDatapackSyncEvent} in some cases per that event's own semantics — sending this
     * twice on join is a harmless, idempotent no-op, not a correctness bug.
     */
    public static void syncDefinitionsToPlayer(ServerPlayer player) {
        PlatformHelper.getInstance().sendHiddenRecipeDefinitionsSync(player, definitionEntries());
    }

    /**
     * Broadcasts every currently-defined hidden recipe's id + hint to every online player —
     * call after a datapack {@code /reload} so connected clients' JEI hint text doesn't go
     * stale until they relog. See each loader's wiring (fabric-1.21.1's
     * {@code ServerLifecycleEvents.END_DATA_PACK_RELOAD}, neoforge-1.21.1's
     * {@code OnDatapackSyncEvent} handler) for where this is actually called from.
     */
    public static void syncDefinitionsToAll(MinecraftServer server) {
        List<HiddenRecipeDefinitionsSyncPayload.Entry> entries = definitionEntries();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlatformHelper.getInstance().sendHiddenRecipeDefinitionsSync(player, entries);
        }
    }

    private static List<HiddenRecipeDefinitionsSyncPayload.Entry> definitionEntries() {
        return HiddenRecipeManager.INSTANCE.all().values().stream()
            .map(HiddenRecipeEvents::toDefinitionEntry)
            .toList();
    }

    private static HiddenRecipeDefinitionsSyncPayload.Entry toDefinitionEntry(HiddenRecipeEntry entry) {
        return new HiddenRecipeDefinitionsSyncPayload.Entry(
            entry.recipe(), entry.hint().translationKey(), entry.hint().showProgress());
    }

    public static void onPlayerLeave(ServerPlayer player) {
        HiddenRecipeManager.INSTANCE.unloadPlayer(player);
        TICK_COUNTERS.remove(player);
        LAST_INVENTORY_FINGERPRINT.remove(player);
    }

    /**
     * Call once per server tick per online player. Throttled internally: every
     * {@value #CHECK_INTERVAL_TICKS} ticks it cheaply fingerprints the player's inventory
     * (only re-testing {@code INVENTORY}-indexed entries if it actually changed) and
     * always re-tests the cheap {@code ADVANCEMENT}/{@code GENERIC} buckets, since testing
     * those conditions is itself O(1)-ish and not worth fingerprinting around.
     */
    public static void onPlayerTick(ServerPlayer player) {
        int counter = TICK_COUNTERS.merge(player, 1, Integer::sum);

        if (counter % AUTOSAVE_INTERVAL_TICKS == 0) {
            HiddenRecipeManager.INSTANCE.flushPlayer(player);
        }

        if (counter % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        int fingerprint = inventoryFingerprint(player);
        Integer last = LAST_INVENTORY_FINGERPRINT.put(player, fingerprint);
        boolean inventoryChanged = last == null || last != fingerprint;

        if (inventoryChanged) {
            notify(player, HiddenRecipeManager.INSTANCE.retest(player, HiddenRecipeTriggerType.INVENTORY));
        }
        notify(player, HiddenRecipeManager.INSTANCE.retest(player,
            HiddenRecipeTriggerType.ADVANCEMENT, HiddenRecipeTriggerType.GENERIC));
    }

    /** Loaders with a real advancement-granted event (e.g. NeoForge) can call this for an instant re-check. */
    public static void onAdvancementGranted(ServerPlayer player) {
        notify(player, HiddenRecipeManager.INSTANCE.retest(player, HiddenRecipeTriggerType.ADVANCEMENT));
    }

    /** Call on server shutdown, before the world unloads. */
    public static void onServerStopping(MinecraftServer server) {
        HiddenRecipeManager.INSTANCE.flushAll(server);
    }

    /**
     * Runs every post-unlock side effect for a set of recipes that just became unlocked for a
     * player: recipe-book grant, client sync, debug log. (The player-facing toast is a
     * separate client-side listener, see the comment below — it fires from this same sync
     * regardless of caller.) Made {@code public} (was {@code private}) so
     * {@code HiddenRecipeCommands}'s {@code /hiddenrecipes unlock} can run the same real
     * unlock path a condition-triggered unlock gets, instead of only updating
     * {@code HiddenRecipeManager}'s bookkeeping — see {@code HiddenRecipeManager#forceUnlock}'s
     * javadoc for the bug this fixes.
     */
    public static void notify(ServerPlayer player, Set<ResourceLocation> newlyUnlocked) {
        if (newlyUnlocked.isEmpty()) {
            return;
        }
        for (ResourceLocation recipeId : newlyUnlocked) {
            Constants.LOGGER.debug("{} unlocked hidden recipe {}", player.getGameProfile().getName(), recipeId);
        }
        grantRecipeBookEntries(player, newlyUnlocked);
        PlatformHelper.getInstance().sendHiddenRecipesSync(player, HiddenRecipeManager.INSTANCE.unlockedFor(player));
        // Player-facing toast is handled client-side, not here: each loader's client entrypoint
        // registers a ClientHiddenRecipeState.addUnlockListener that diffs this sync against the
        // previously-known set and toasts only the newly-added recipes (see fabric-1.21.1's and
        // neoforge-1.21.1's HiddenRecipeUnlockToasts). Kept out of this server-side class since it
        // has no client-only Minecraft classes to depend on, by design (see Constants/PlatformHelper
        // split elsewhere in this file).
    }

    /**
     * Grants the vanilla recipe book entry for each given recipe id, so the recipe actually
     * shows up in the player's in-game recipe book (crafting table / furnace-family "book"
     * panel) and can be clicked to auto-fill — not just be craftable if the player already
     * knows the pattern by hand. This was a real gap found auditing the mod: unlocking a
     * hidden recipe previously only updated {@code HiddenRecipeManager}'s own bookkeeping
     * (stopping the enforcement mixins from blocking it) and told JEI to show the real recipe
     * — nothing ever called vanilla's own recipe-book-unlock API, so the recipe book itself
     * would never have shown it even after unlocking. Fixed by calling
     * {@link ServerPlayer#awardRecipes} directly (pure vanilla API, no platform abstraction
     * needed, unlike networking).
     *
     * <p>Idempotent — awarding an already-known recipe is a safe no-op — so this is called
     * unconditionally both on join (to backfill anyone who unlocked something in a session
     * before this fix existed, or simply to guarantee vanilla's own recipe-book save state
     * matches our own on every reconnect) and on every fresh unlock.
     *
     * <p>NEEDS VERIFICATION (no build access): {@code ServerPlayer.awardRecipes(Collection
     * <RecipeHolder<?>>)} is believed to be the correct, long-standing vanilla API for this
     * (used internally by advancement recipe-rewards and the {@code /recipe give} command),
     * chosen over a possibly-nonexistent {@code awardRecipesByKey(List<ResourceLocation>)}
     * convenience overload since I'm less certain that one exists. Note this likely also
     * triggers vanilla's own recipe-book "unlocked" toast/highlight animation — separate from,
     * and probably firing alongside, this mod's own "Hidden Recipe Unlocked!" toast
     * ({@code HiddenRecipeUnlockToasts}). Not treated as a conflict to resolve: the two serve
     * different purposes (vanilla's is the generic per-recipe book-state indicator; this mod's
     * is the specific "you just cracked a secret" narrative moment), but worth knowing about
     * rather than being surprised by two notifications on unlock.
     */
    private static void grantRecipeBookEntries(ServerPlayer player, Collection<ResourceLocation> recipeIds) {
        if (recipeIds.isEmpty()) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        List<RecipeHolder<?>> holders = new ArrayList<>();
        for (ResourceLocation recipeId : recipeIds) {
            server.getRecipeManager().byKey(recipeId).ifPresent(holders::add);
        }
        if (!holders.isEmpty()) {
            player.awardRecipes(holders);
        }
    }

    private static int inventoryFingerprint(ServerPlayer player) {
        int hash = 1;
        for (ItemStack stack : player.getInventory().items) {
            hash = 31 * hash + stackFingerprint(stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            hash = 31 * hash + stackFingerprint(stack);
        }
        return hash;
    }

    private static int stackFingerprint(ItemStack stack) {
        return stack.isEmpty() ? 0 : Objects.hash(stack.getItem(), stack.getCount(), stack.getComponentsPatch());
    }

    private HiddenRecipeEvents() {
    }
}
