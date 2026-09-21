package com.craisinlord.hiddenrecipes.mixin;

import com.craisinlord.hiddenrecipes.HiddenRecipeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Blocks manual/legitimate-looking smelting of a still-locked hidden recipe in furnaces,
 * blast furnaces, and smokers (all three share this base class, same one-mixin-covers-three
 * pattern as {@code AbstractFurnaceBlockMixin}) — the smelting-family counterpart to
 * {@code CraftingMenuMixin}.
 *
 * <p>Cancels the block entity's entire tick when the current input matches a hidden recipe
 * the tracked opener ({@link FurnaceInteractionTracker}) hasn't unlocked, rather than trying
 * to surgically suppress just the burn step — coarser, but doesn't depend on knowing the
 * exact private helper methods vanilla uses internally to decide whether to burn. The
 * practical effect: a locked item just sits in the furnace inert (no progress, no fuel
 * consumed) until removed or unlocked. One accepted side effect: if the furnace was already
 * lit from a prior legitimate smelt and a locked item is then inserted, the lit-fuel
 * countdown also pauses rather than continuing to burn down — treated as a harmless (if
 * anything, player-favorable) quirk of the coarse cancel, not worth chasing precision on.
 *
 * <p><b>KNOWN, ACCEPTED GAP:</b> if {@link FurnaceInteractionTracker} has no recorded opener
 * for this block entity (e.g. it's only ever been fed by a hopper, never manually opened),
 * enforcement is skipped entirely and the recipe smelts normally — there is no player to
 * test the unlock condition against. This was an explicit scope decision, not an oversight:
 * documented in README.md's "Known limitations", same treatment as the pre-existing
 * multiplayer-JEI-sync gap already documented on {@code HiddenRecipesJeiPlugin}.
 *
 * <p><b>Partially verified this pass</b> against public Mojmap javadoc mirrors (see README
 * "Build status"):
 * <ul>
 *   <li>{@code quickCheck} — <b>confirmed</b> to exist, as
 *       {@code private final RecipeManager.CachedCheck<SingleRecipeInput, ? extends
 *       AbstractCookingRecipe> quickCheck}. This replaces an earlier, wrong guess of a
 *       {@code recipeType} field — using the real cache field is also strictly better than
 *       that draft's plan of independently re-deriving the match via a raw
 *       {@code RecipeManager.getRecipeFor} call, since this now asks vanilla's own cached
 *       check the exact same question it's about to ask itself.</li>
 *   <li>{@code RecipeManager.CachedCheck.getRecipeFor(C, ServerLevel)} — the interface and its
 *       {@code createCheck} factory are confirmed to exist; this exact method name on the
 *       interface itself is inferred from context (mirrors {@code RecipeManager}'s own method
 *       name) but not independently corroborated — check this one specifically if the build
 *       fails here.</li>
 *   <li>{@code serverTick(Level, BlockPos, BlockState, AbstractFurnaceBlockEntity)} — believed
 *       stable and unchanged since the ~1.17 block-entity-ticker refactor, not directly
 *       re-verified this pass but lower risk than the above.</li>
 *   <li>{@code items} — believed to be the {@code NonNullList<ItemStack>} backing all three
 *       slots (0=input, 1=fuel, 2=output); not directly re-verified this pass.</li>
 * </ul>
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Shadow
    @Final
    protected NonNullList<ItemStack> items;

    @Shadow
    @Final
    private RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck;

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void hiddenrecipes$blockLockedSmelting(
        Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        AbstractFurnaceBlockEntityMixin self = (AbstractFurnaceBlockEntityMixin) (Object) blockEntity;
        ItemStack input = self.items.get(0);
        if (input.isEmpty()) {
            return;
        }

        ServerPlayer lastOpener = FurnaceInteractionTracker.lastOpener(blockEntity);
        if (lastOpener == null) {
            // No attributable player — known, documented gap, see class javadoc. Let it smelt.
            return;
        }

        Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> matched =
            self.quickCheck.getRecipeFor(new SingleRecipeInput(input), serverLevel);

        matched.ifPresent(holder -> {
            ResourceLocation recipeId = holder.id();
            if (HiddenRecipeManager.INSTANCE.isLocked(recipeId, lastOpener)) {
                ci.cancel();
            }
        });
    }
}
