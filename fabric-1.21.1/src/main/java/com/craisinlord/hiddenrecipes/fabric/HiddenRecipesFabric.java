package com.craisinlord.hiddenrecipes.fabric;

import com.craisinlord.hiddenrecipes.HiddenRecipeCommands;
import com.craisinlord.hiddenrecipes.HiddenRecipeEvents;
import com.craisinlord.hiddenrecipes.HiddenRecipes;
import com.craisinlord.hiddenrecipes.network.HiddenRecipeDefinitionsSyncPayload;
import com.craisinlord.hiddenrecipes.network.HiddenRecipesSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;

public class HiddenRecipesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        HiddenRecipes.init();

        PayloadTypeRegistry.playS2C().register(HiddenRecipesSyncPayload.TYPE, HiddenRecipesSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(HiddenRecipeDefinitionsSyncPayload.TYPE, HiddenRecipeDefinitionsSyncPayload.STREAM_CODEC);

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricHiddenRecipeReloadListener());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            HiddenRecipeCommands.register(dispatcher));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            HiddenRecipeEvents.onPlayerJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            HiddenRecipeEvents.onPlayerLeave(handler.getPlayer()));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                HiddenRecipeEvents.onPlayerTick(player);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(HiddenRecipeEvents::onServerStopping);

        // Broadcasts updated hidden-recipe hint definitions to everyone already connected
        // after a /reload — otherwise their JEI hint text would silently go stale until they
        // relog. Doesn't fire on initial world/server startup reloads (success is still true
        // then, but the player list is empty, so the loop below is a harmless no-op).
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                HiddenRecipeEvents.syncDefinitionsToAll(server);
            }
        });
    }
}
