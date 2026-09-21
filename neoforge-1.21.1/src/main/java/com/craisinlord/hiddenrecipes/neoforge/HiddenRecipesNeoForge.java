package com.craisinlord.hiddenrecipes.neoforge;

import com.craisinlord.hiddenrecipes.network.ClientHiddenRecipeDefinitions;
import com.craisinlord.hiddenrecipes.network.ClientHiddenRecipeState;
import com.craisinlord.hiddenrecipes.network.HiddenRecipeDefinitionsSyncPayload;
import com.craisinlord.hiddenrecipes.network.HiddenRecipesSyncPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import com.craisinlord.hiddenrecipes.Constants;
import com.craisinlord.hiddenrecipes.HiddenRecipes;

@Mod(Constants.MOD_ID)
public class HiddenRecipesNeoForge {
    public HiddenRecipesNeoForge(IEventBus modEventBus) {
        HiddenRecipes.init();
        NeoForge.EVENT_BUS.register(new HiddenRecipeNeoForgeEvents());
        modEventBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(Constants.MOD_ID).playToClient(
            HiddenRecipesSyncPayload.TYPE,
            HiddenRecipesSyncPayload.STREAM_CODEC,
            (payload, context) -> ClientHiddenRecipeState.set(payload.unlocked()));
        event.registrar(Constants.MOD_ID).playToClient(
            HiddenRecipeDefinitionsSyncPayload.TYPE,
            HiddenRecipeDefinitionsSyncPayload.STREAM_CODEC,
            (payload, context) -> ClientHiddenRecipeDefinitions.set(payload.entries()));
    }
}
