package com.craisinlord.hiddenrecipes.neoforge;

import com.craisinlord.hiddenrecipes.HiddenRecipeCommands;
import com.craisinlord.hiddenrecipes.HiddenRecipeEvents;
import com.craisinlord.hiddenrecipes.data.HiddenRecipeReloadListener;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class HiddenRecipeNeoForgeEvents {

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new HiddenRecipeReloadListener());
    }

    /**
     * Broadcasts updated hint definitions after a {@code /reload}. Believed (per public
     * research this pass, not independently re-confirmed against decompiled source) to also
     * fire on player join with {@code getPlayer()} set to that player — deliberately ignored
     * here ({@code getPlayer() != null} returns early) since {@code HiddenRecipeEvents.onPlayerJoin}
     * (common code, shared with Fabric) already handles the per-player join-time sync; only
     * the null-player "sync everyone" case is handled here to avoid sending it twice on join.
     */
    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        HiddenRecipeEvents.syncDefinitionsToAll(event.getPlayerList().getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        HiddenRecipeCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            HiddenRecipeEvents.onPlayerJoin(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            HiddenRecipeEvents.onPlayerLeave(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            HiddenRecipeEvents.onAdvancementGranted(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            HiddenRecipeEvents.onPlayerTick(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        HiddenRecipeEvents.onServerStopping(event.getServer());
    }
}
