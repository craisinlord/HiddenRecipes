package com.craisinlord.hiddenrecipes.integration.jei;

import com.craisinlord.hiddenrecipes.Constants;
import com.craisinlord.hiddenrecipes.HiddenRecipeManager;
import com.craisinlord.hiddenrecipes.condition.impl.AndCondition;
import com.craisinlord.hiddenrecipes.data.HiddenRecipeEntry;
import com.craisinlord.hiddenrecipes.network.ClientHiddenRecipeDefinitions;
import com.craisinlord.hiddenrecipes.network.ClientHiddenRecipeState;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Wired into fabric-1.21.1/neoforge-1.21.1 via {@code src/jei/NOTES.md}'s steps — Fabric
 * discovers it through the {@code jei_mod_plugin} entrypoint in {@code fabric.mod.json}
 * (verified against JEI's own fabric.mod.json, not guessed — JEI does NOT use
 * {@code META-INF/services} on Fabric the way {@code PlatformHelper} does), NeoForge finds
 * it via the {@code @JeiPlugin} annotation below (its own classpath scan, no explicit
 * registration file needed there).
 *
 * <p>UPDATED: previously this read {@link HiddenRecipeManager#all()} directly everywhere,
 * which only works in singleplayer/integrated-server play (same JVM as the server's
 * populated singleton) and left this list permanently empty on a remote dedicated server,
 * since datapack content itself was never synced to the client. That's now fixed for hint
 * *text*: {@link #resolveVisibleEntries()} falls back to
 * {@link ClientHiddenRecipeDefinitions} — populated by a new, separate sync payload sent on
 * join and after {@code /reload} — whenever the direct server-side singleton is empty. See
 * that class's javadoc for exactly what is and isn't synced this way.
 *
 * <p><b>STILL A KNOWN GAP — narrower than before:</b> live progress display ("X/Y
 * requirements met") remains singleplayer/integrated-server-only, because it needs the real
 * {@code HiddenRecipeCondition} tree to test, and conditions are deliberately not synced to
 * the client (see {@code HiddenRecipeDefinitionsSyncPayload}'s javadoc for why). This is
 * already handled gracefully — {@code HiddenRecipeHintCategory#resolveProgress} already
 * returns no progress line whenever there's no reachable integrated server, which is true of
 * every remote connection regardless of this fix.
 *
 * <p><b>UNVERIFIED ORDERING CAVEAT (no build access to check event/packet ordering this
 * pass):</b> {@link #resolveVisibleEntries()} needs {@link ClientHiddenRecipeDefinitions} to
 * already be populated by the time JEI calls {@link #registerRecipes}/
 * {@link #registerRecipeCatalysts} after connecting to a remote server. This relies on the
 * definitions-sync packet (sent from {@code HiddenRecipeEvents#onPlayerJoin}) arriving before
 * JEI's own runtime-ready callback fires for that connection — plausible given JEI is known
 * to re-initialize per world/server join, but not something confirmable without actually
 * running the game. If hint text is still missing on a fresh remote connection (but appears
 * after, say, an inventory screen reopen), this ordering assumption is the first thing to
 * check.
 *
 * <p><b>FIXED THIS PASS — real-recipe hiding is now generic across every recipe type, not
 * crafting-table-only.</b> A previous pass found and explicitly declined to fix a real gap
 * here: real-recipe hiding only ever covered crafting-table recipes, so a hard-enforced
 * furnace-family hidden recipe would stay mixin-blocked from being crafted while still
 * showing its true ingredients in JEI's smelting/blasting/smoking category. The fix that was
 * deferred back then (typed JEI {@code RecipeType} constants for smelting/blasting/smoking,
 * hand-guessing each one's exact generic parameter) turned out to be the wrong shape of fix
 * entirely. {@link #hideOrShowRealRecipe} instead resolves each hidden recipe's actual
 * {@code RecipeHolder} once via vanilla's own type-agnostic {@code RecipeManager#byKey}, reads
 * that recipe's own {@code getType()} back off the holder, and feeds both straight into
 * {@code RecipeType.createFromVanilla(...)} — using raw types (matching the erasure-based
 * pattern {@link #assemble(RecipeHolder)} below already used, not a new technique introduced
 * here) so the exact generic parameter of any given vanilla {@code RecipeType} constant never
 * has to be known or written out by hand. This isn't a guess: a {@code Recipe}'s
 * {@code getType()} is contractually guaranteed by vanilla to return the exact
 * {@code RecipeType} it was registered under, so the type used to build the JEI wrapper is
 * always the recipe's real one, not an assumption about it.
 *
 * <p>Net effect, found researching JEI's actual runtime API (its own GitHub wiki's "Runtime
 * JEI Integration" page, which documents {@code IRecipeManager.hideRecipes(RecipeType<T>,
 * List<T>)} as the one and only real hide/unhide shape — there is no bare hide-by-id overload
 * in JEI itself; KubeJS's own "hide any recipe by id" scripting API is KubeJS's own adapter
 * built on top of this same typed call, not a JEI feature this plugin could call into
 * directly): this now covers every hidden recipe regardless of its vanilla recipe type —
 * crafting, the furnace family, smithing table, stonecutter — with no per-type code at all.
 * It also, as a consequence rather than a deliberate new feature, covers any third-party
 * recipe registered the normal way under vanilla's {@code RecipeType} registry (confirmed
 * earlier in this project's own research to include Create's kinetic-machine recipes and
 * likely Ars Nouveau's Enchanting Apparatus) — real-recipe hiding for those was never
 * attempted before at all, since only crafting was ever handled. This is real-recipe
 * *hiding* only; it changes nothing about hard-block *enforcement*, which stays exactly
 * crafting table + furnace family per README's "Enforcement scope" — a third-party mod's
 * recipe can now correctly disappear from JEI while locked, but still remains craftable by
 * hand the same as before, because there is still no universal interception point to block
 * it with.
 */
@JeiPlugin
public final class HiddenRecipesJeiPlugin implements IModPlugin {

    private static volatile IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        ClientHiddenRecipeState.addChangeListener(HiddenRecipesJeiPlugin::refreshVisibility);
        refreshVisibility();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /**
     * Registered as a {@link ClientHiddenRecipeState} change listener in
     * {@link #onRuntimeAvailable} — swaps which of {real recipe, hint page} JEI shows for
     * each hidden recipe's output item whenever the synced unlocked set changes.
     *
     * <p>Real-recipe hiding is done one entry at a time via {@link #hideOrShowRealRecipe}
     * rather than batched into one {@code hideRecipes} call per vanilla recipe type the way
     * the old crafting-only version batched all crafting recipes together. That's a
     * deliberate simplicity-over-micro-optimization tradeoff: batching by type here would
     * mean grouping entries into a {@code Map<RecipeType<?>, List<RecipeHolder<?>>>} first,
     * which adds real complexity for a call that only ever runs on an unlock-state change
     * (not per frame) over what's realistically a handful of hidden recipes in any real
     * modpack — not worth it.
     */
    private static void refreshVisibility() {
        IJeiRuntime jeiRuntime = runtime;
        if (jeiRuntime == null) {
            return;
        }
        mezz.jei.api.recipe.IRecipeManager recipeManager = jeiRuntime.getRecipeManager();
        List<HiddenRecipeEntry> toHideHint = new ArrayList<>();
        List<HiddenRecipeEntry> toShowHint = new ArrayList<>();

        for (HiddenRecipeEntry entry : resolveVisibleEntries()) {
            boolean unlocked = ClientHiddenRecipeState.isUnlocked(entry.recipe());
            resolveHolder(entry.recipe()).ifPresent(holder -> hideOrShowRealRecipe(recipeManager, holder, unlocked));
            (unlocked ? toHideHint : toShowHint).add(entry);
        }

        if (!toHideHint.isEmpty()) {
            recipeManager.hideRecipes(HiddenRecipeHintCategory.TYPE, toHideHint);
        }
        if (!toShowHint.isEmpty()) {
            recipeManager.unhideRecipes(HiddenRecipeHintCategory.TYPE, toShowHint);
        }
    }

    /**
     * Hides or unhides one recipe's real ingredients in JEI, regardless of what vanilla
     * recipe type it is — see the class javadoc for why raw types are used here instead of a
     * per-type generic constant, and why that's safe rather than a guess. {@code holder}'s
     * own {@link Recipe#getType()} is the single source of truth for which JEI category this
     * targets; nothing here assumes or hard-codes a specific recipe type.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void hideOrShowRealRecipe(mezz.jei.api.recipe.IRecipeManager recipeManager, RecipeHolder<?> holder, boolean unlocked) {
        Recipe recipe = holder.value();
        net.minecraft.world.item.crafting.RecipeType vanillaType = recipe.getType();
        mezz.jei.api.recipe.RecipeType jeiType = RecipeType.createFromVanilla(vanillaType);
        List list = List.of(holder);
        if (unlocked) {
            recipeManager.unhideRecipes(jeiType, list);
        } else {
            recipeManager.hideRecipes(jeiType, list);
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new HiddenRecipeHintCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(HiddenRecipeHintCategory.TYPE, resolveVisibleEntries());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (HiddenRecipeEntry entry : resolveVisibleEntries()) {
            resolveOutput(entry.recipe()).ifPresent(output ->
                registration.addRecipeCatalyst(output, HiddenRecipeHintCategory.TYPE));
        }
    }

    /**
     * A shared placeholder condition for entries reconstructed from
     * {@link ClientHiddenRecipeDefinitions} (remote-server fallback, see class javadoc) —
     * an empty {@code and} is vacuously true and has no leaves, so it's safe to hand around
     * even though it's never meaningfully tested: {@code HiddenRecipeHintCategory}'s progress
     * resolution already bails out before testing anything whenever there's no reachable
     * integrated server, which is exactly when this placeholder is ever used.
     */
    private static final AndCondition REMOTE_PLACEHOLDER_CONDITION = new AndCondition(List.of());

    /**
     * Prefers the real, server-populated {@link HiddenRecipeManager#all()} when it's
     * non-empty (covers singleplayer/integrated-server play unchanged, including live
     * progress display), and falls back to reconstructing entries from the synced
     * {@link ClientHiddenRecipeDefinitions} cache otherwise (remote dedicated server —
     * hint text works, progress doesn't, see class javadoc).
     */
    private static List<HiddenRecipeEntry> resolveVisibleEntries() {
        var direct = HiddenRecipeManager.INSTANCE.all().values();
        if (!direct.isEmpty()) {
            return List.copyOf(direct);
        }
        return ClientHiddenRecipeDefinitions.all().entrySet().stream()
            .map(e -> new HiddenRecipeEntry(e.getKey(), REMOTE_PLACEHOLDER_CONDITION, e.getValue()))
            .toList();
    }

    /** Recipe *existence* (unlike hidden_recipes conditions/hints) is synced to every client, so this works remotely. */
    private static Optional<ItemStack> resolveOutput(ResourceLocation recipeId) {
        if (Minecraft.getInstance().level == null) {
            return Optional.empty();
        }
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        return recipeManager.byKey(recipeId).map(HiddenRecipesJeiPlugin::assemble);
    }

    /** Type-agnostic recipe lookup — unlike the old crafting-only version, works for any recipe type. */
    private static Optional<RecipeHolder<?>> resolveHolder(ResourceLocation recipeId) {
        if (Minecraft.getInstance().level == null) {
            return Optional.empty();
        }
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        return recipeManager.byKey(recipeId);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ItemStack assemble(RecipeHolder<?> holder) {
        Recipe recipe = holder.value();
        return recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
    }
}
