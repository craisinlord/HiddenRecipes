package com.craisinlord.hiddenrecipes.neoforge;

import com.craisinlord.hiddenrecipes.Constants;
import com.craisinlord.hiddenrecipes.network.ClientHiddenRecipeDefinitions;
import com.craisinlord.hiddenrecipes.network.ClientHiddenRecipeState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * {@code value = Dist.CLIENT} makes FML only load/register this on the client — safe to
 * reference client-only event classes here even though the mod jar also runs on dedicated
 * servers.
 */
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class HiddenRecipeNeoForgeClientEvents {

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientHiddenRecipeState.clear();
        ClientHiddenRecipeDefinitions.clear();
    }

    private HiddenRecipeNeoForgeClientEvents() {
    }
}
