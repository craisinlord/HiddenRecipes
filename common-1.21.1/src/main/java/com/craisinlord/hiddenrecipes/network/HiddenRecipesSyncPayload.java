package com.craisinlord.hiddenrecipes.network;

import com.craisinlord.hiddenrecipes.Constants;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: the full set of hidden recipes this player currently has unlocked.
 * Sent on join (after {@code HiddenRecipePlayerDataStorage} loads their save) and again
 * whenever {@link com.craisinlord.hiddenrecipes.HiddenRecipeManager#retest} unlocks
 * something new. Always sends the whole set rather than a delta — the set is small
 * (hidden recipes are meant to be rare) and "just resend everything" avoids any
 * lost-packet-causes-permanent-desync class of bug.
 */
public record HiddenRecipesSyncPayload(List<ResourceLocation> unlocked) implements CustomPacketPayload {

    public static final Type<HiddenRecipesSyncPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_unlocked"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, HiddenRecipesSyncPayload> STREAM_CODEC =
        ResourceLocation.STREAM_CODEC
            .apply(ByteBufCodecs.<io.netty.buffer.ByteBuf, ResourceLocation, List<ResourceLocation>>collection(ArrayList::new))
            .map(HiddenRecipesSyncPayload::new, HiddenRecipesSyncPayload::unlocked);

    @Override
    public Type<HiddenRecipesSyncPayload> type() {
        return TYPE;
    }
}
