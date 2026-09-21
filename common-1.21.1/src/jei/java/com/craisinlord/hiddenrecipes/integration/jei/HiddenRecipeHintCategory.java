package com.craisinlord.hiddenrecipes.integration.jei;

import com.craisinlord.hiddenrecipes.Constants;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeCondition;
import com.craisinlord.hiddenrecipes.data.HiddenRecipeEntry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * The "???" recipe page JEI shows for a hidden recipe's output item instead of the real
 * recipe. This is what makes hitting JEI's recipe-lookup key (default R) on such an item
 * show a hint rather than nothing (fully hidden) or the real ingredients (not hidden
 * enough) — the real recipe stays hidden via {@link HiddenRecipesJeiPlugin#hideRecipes},
 * and this category is what R-key lookup resolves to for the same output item instead.
 */
public final class HiddenRecipeHintCategory implements IRecipeCategory<HiddenRecipeEntry> {

    public static final RecipeType<HiddenRecipeEntry> TYPE = RecipeType.create(
        Constants.MOD_ID, "hidden_recipe_hint", HiddenRecipeEntry.class);

    private static final int TEXT_COLOR = 0xFF404040;
    private static final int PROGRESS_COLOR = 0xFF808080;
    private static final int PADDING = 6;

    private final IDrawable icon;

    public HiddenRecipeHintCategory(IGuiHelper guiHelper) {
        // No dedicated icon texture exists yet (no art pipeline for this mod — see
        // README "Known limitations"), so this draws a plain "?" glyph in place of a real
        // icon rather than shipping a genuinely blank tab. Swap for
        // guiHelper.createDrawableItemStack(...)/a real texture once art exists — this is
        // the one spot that needs updating.
        this.icon = new QuestionMarkIcon();
    }

    @Override
    public RecipeType<HiddenRecipeEntry> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("category.hidden_recipes.hint");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 60;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HiddenRecipeEntry entry, IFocusGroup focuses) {
        // Deliberately no INPUT slots — the point is that the ingredients stay unknown.
        // But an OUTPUT slot for the crafted result IS needed: it's the standard JEI
        // mechanism that associates this category with that item for the recipe-lookup
        // key (R). Without it, JEI had no output to index this category under, so R on
        // the crafted item found nothing here — the hint only ever surfaced indirectly,
        // via the (now-removed) catalyst registration's Uses-key (U) association, which
        // is exactly backwards from the intended "R on the result, not U on an
        // ingredient" behavior. Revealing just the output item here tells the player
        // nothing they don't already know (they were looking it up by name/icon), unlike
        // an input slot, which would leak the real ingredients.
        //
        // Progress-hint rendering (e.g. "2/3 requirements met") reads live player state,
        // so it belongs in #draw (called every frame) rather than here (built once).
        resolveOutput(entry.recipe()).ifPresent(stack -> builder.addOutputSlot(getWidth() / 2 - 8, getHeight() - 24).addItemStack(stack));
    }

    private static java.util.Optional<ItemStack> resolveOutput(ResourceLocation recipeId) {
        if (Minecraft.getInstance().level == null) {
            return java.util.Optional.empty();
        }
        return Minecraft.getInstance().level.getRecipeManager().byKey(recipeId).map(HiddenRecipeHintCategory::assemble);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ItemStack assemble(RecipeHolder<?> holder) {
        Recipe recipe = holder.value();
        return recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
    }

    @Override
    public void draw(HiddenRecipeEntry entry, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        int maxWidth = getWidth() - PADDING * 2;

        // Component implements FormattedText, so the translated hint text can be word-wrapped
        // directly without any conversion.
        Component hint = entry.hint().text();
        guiGraphics.drawWordWrap(font, hint, PADDING, PADDING, maxWidth, TEXT_COLOR);
        int y = PADDING + font.getSplitter().splitLines(hint, maxWidth, Style.EMPTY).size() * font.lineHeight + 2;

        if (entry.hint().showProgress()) {
            ProgressText progress = resolveProgress(entry.condition());
            if (progress != null) {
                Component progressText = Component.translatable(
                    "category.hidden_recipes.hint.progress", progress.met(), progress.total());
                guiGraphics.drawWordWrap(font, progressText, PADDING, y, maxWidth, PROGRESS_COLOR);
            }
            // If progress can't be resolved (not singleplayer / no integrated server —
            // see HiddenRecipesJeiPlugin's remote-play javadoc for why), we simply skip the
            // progress line rather than show something misleading or crash.
        }
    }

    /**
     * Re-tests every leaf of the condition tree against the viewing player and reports how
     * many currently pass. Only works when a {@link ServerPlayer} instance for the viewer is
     * reachable, which — same limitation documented on {@link HiddenRecipesJeiPlugin} — is
     * only the case in singleplayer/integrated-server play, since conditions test against
     * {@link ServerPlayer} and a remote dedicated server never hands the client one.
     */
    private static ProgressText resolveProgress(HiddenRecipeCondition condition) {
        // Mojmap calls the integrated-server accessor getSingleplayerServer(); it is non-null
        // only when this client is also hosting the world.
        MinecraftServer integratedServer = Minecraft.getInstance().getSingleplayerServer();
        if (integratedServer == null) {
            return null;
        }
        var localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            return null;
        }
        ServerPlayer serverPlayer = integratedServer.getPlayerList().getPlayer(localPlayer.getUUID());
        if (serverPlayer == null) {
            return null;
        }

        List<HiddenRecipeCondition> leaves = condition.leaves();
        int met = 0;
        for (HiddenRecipeCondition leaf : leaves) {
            if (leaf.test(serverPlayer)) {
                met++;
            }
        }
        return new ProgressText(met, leaves.size());
    }

    private record ProgressText(int met, int total) {
    }

    /** Draws a plain "?" glyph as a stand-in recipe-category icon — see the constructor comment. */
    private static final class QuestionMarkIcon implements IDrawable {
        @Override
        public int getWidth() {
            return 16;
        }

        @Override
        public int getHeight() {
            return 16;
        }

        @Override
        public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
            Font font = Minecraft.getInstance().font;
            String glyph = "?";
            int textWidth = font.width(glyph);
            guiGraphics.drawString(font, glyph,
                xOffset + (getWidth() - textWidth) / 2, yOffset + (getHeight() - font.lineHeight) / 2,
                TEXT_COLOR, false);
        }
    }

    public static ResourceLocation recipeIdFor(HiddenRecipeEntry entry) {
        return entry.recipe();
    }
}
