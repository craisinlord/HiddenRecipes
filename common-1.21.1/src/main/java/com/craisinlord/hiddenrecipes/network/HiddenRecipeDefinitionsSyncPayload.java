package com.craisinlord.hiddenrecipes.network;

import com.craisinlord.hiddenrecipes.Constants;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: which recipes are hidden at all, and each one's hint (translation key +
 * show_progress), independent of any particular player's unlocked state. This is the piece
 * that was missing for remote dedicated-server play — {@link HiddenRecipesSyncPayload}
 * (unlocked-id set) already synced fine, but until this payload existed, a client connected
 * to a real dedicated server had no way to learn a hidden recipe's definition even existed,
 * since {@code HiddenRecipeManager.INSTANCE} is a server-side singleton the client never
 * shares outside of singleplayer/integrated-server play.
 *
 * <p>Deliberately does NOT carry the unlock {@code condition} tree — only enough for JEI to
 * render the hint page's title/text. Conditions test against a real {@code ServerPlayer} and
 * are meant to be evaluated server-side only; shipping them to the client would either be
 * inert (nothing client-side can safely call {@code test()}) or, if a client-side evaluator
 * were built, a spot for a modified client to lie about its own state. Live progress display
 * ("X/Y requirements met") therefore remains singleplayer/integrated-server-only even after
 * this fix — see {@code HiddenRecipesJeiPlugin}'s updated javadoc for the narrowed remaining
 * gap, and README.md's "Known limitations".
 *
 * <p>Sent to a joining player alongside {@link HiddenRecipesSyncPayload} (see
 * {@code HiddenRecipeEvents#onPlayerJoin}), and broadcast to all online players after a
 * datapack {@code /reload} (see each loader's wiring — Fabric's
 * {@code ServerLifecycleEvents.END_DATA_PACK_RELOAD}, NeoForge's {@code OnDatapackSyncEvent}
 * with a null {@code getPlayer()}) so hint text stays correct without needing a relog.
 */
public record HiddenRecipeDefinitionsSyncPayload(List<Entry> entries) implements CustomPacketPayload {

    public static final Type<HiddenRecipeDefinitionsSyncPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_definitions"));

    /** One hidden recipe's client-visible identity: which recipe, and how to hint at it. */
    public record Entry(ResourceLocation recipe, String translationKey, boolean showProgress) {
        public static final StreamCodec<io.netty.buffer.ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Entry::recipe,
            ByteBufCodecs.STRING_UTF8, Entry::translationKey,
            ByteBufCodecs.BOOL, Entry::showProgress,
            Entry::new
        );
    }

    public static final StreamCodec<io.netty.buffer.ByteBuf, HiddenRecipeDefinitionsSyncPayload> STREAM_CODEC =
        Entry.STREAM_CODEC
            .apply(ByteBufCodecs.<io.netty.buffer.ByteBuf, Entry, List<Entry>>collection(ArrayList::new))
            .map(HiddenRecipeDefinitionsSyncPayload::new, HiddenRecipeDefinitionsSyncPayload::entries);

    @Override
    public Type<HiddenRecipeDefinitionsSyncPayload> type() {
        return TYPE;
    }
}
