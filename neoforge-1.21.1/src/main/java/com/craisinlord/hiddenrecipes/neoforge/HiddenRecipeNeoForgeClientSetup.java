package com.craisinlord.hiddenrecipes.neoforge;

import com.craisinlord.hiddenrecipes.Constants;
import com.craisinlord.hiddenrecipes.network.ClientHiddenRecipeState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Separate from {@link HiddenRecipeNeoForgeClientEvents} because that class's
 * {@code onLoggingOut} handler is a {@code ClientPlayerNetworkEvent} (game bus), while
 * {@link FMLClientSetupEvent} is a mod-bus event ({@code IModBusEvent}) — the two used to
 * need different {@code @EventBusSubscriber(bus = ...)} values and so couldn't share one
 * annotated class.
 *
 * <p>Verified this pass (see README "Build status"): as of NeoForge 1.21,
 * {@code @EventBusSubscriber}'s {@code bus} parameter is deprecated — NeoForge now
 * auto-detects the correct bus from the event type used in each {@code @SubscribeEvent}
 * method, and explicitly passing {@code bus = Bus.GAME}/{@code Bus.MOD} logs a removal
 * warning. So {@code bus} is deliberately omitted below (matching how the existing,
 * pre-this-project {@code HiddenRecipeNeoForgeClientEvents} already omits it) rather than
 * set to {@code Bus.MOD} as an earlier draft of this file did.
 */
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class HiddenRecipeNeoForgeClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientHiddenRecipeState.addUnlockListener(HiddenRecipeUnlockToasts::show);
    }

    private HiddenRecipeNeoForgeClientSetup() {
    }
}
