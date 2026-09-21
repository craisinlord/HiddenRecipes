package com.craisinlord.hiddenrecipes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Records which player manually opened a furnace/blast-furnace/smoker, feeding
 * {@link FurnaceInteractionTracker} for {@code AbstractFurnaceBlockEntityMixin} to read when
 * deciding whether to block a hidden smelting recipe (see that class and
 * {@link FurnaceInteractionTracker} for the full rationale).
 *
 * <p>Targets the shared {@code AbstractFurnaceBlock} base (covers all three furnace types
 * at once, mirroring how {@code CraftingMenuMixin} covers both crafting grids with one
 * mixin) rather than the menu's constructor, specifically to avoid needing to match an
 * uncertain constructor parameter list — a block's empty-hand-interaction method hands the
 * interacting player straight to us as a method parameter.
 *
 * <p><b>NEEDS VERIFICATION — no build access this session.</b>
 * {@code useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)} is believed to
 * be the correct method (the empty-hand block-interaction entrypoint since the "split use"
 * refactor that separated it from item-in-hand interactions), but should be confirmed
 * against decompiled 1.21.1 source — if furnace's right-click-to-open-GUI handling lives on
 * a differently-named or differently-shaped method in this version, this target needs
 * updating accordingly.
 */
@Mixin(AbstractFurnaceBlock.class)
public abstract class AbstractFurnaceBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"))
    private void hiddenrecipes$recordOpener(
        BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
        CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide()) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof Container container) {
            FurnaceInteractionTracker.recordOpener(container, serverPlayer);
        }
    }
}
