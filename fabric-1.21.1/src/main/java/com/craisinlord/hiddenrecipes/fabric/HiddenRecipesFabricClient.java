package com.craisinlord.hiddenrecipes.fabric;

import com.craisinlord.hiddenrecipes.network.ClientHiddenRecipeDefinitions;
import com.craisinlord.hiddenrecipes.network.ClientHiddenRecipeState;
import com.craisinlord.hiddenrecipes.network.HiddenRecipeDefinitionsSyncPayload;
import com.craisinlord.hiddenrecipes.network.HiddenRecipesSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class HiddenRecipesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(HiddenRecipesSyncPayload.TYPE,
            (payload, context) -> ClientHiddenRecipeState.set(payload.unlocked()));
        ClientPlayNetworking.registerGlobalReceiver(HiddenRecipeDefinitionsSyncPayload.TYPE,
            (payload, context) -> ClientHiddenRecipeDefinitions.set(payload.entries()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientHiddenRecipeState.clear();
            ClientHiddenRecipeDefinitions.clear();
        });

        ClientHiddenRecipeState.addUnlockListener(HiddenRecipeUnlockToasts::show);
    }
}
