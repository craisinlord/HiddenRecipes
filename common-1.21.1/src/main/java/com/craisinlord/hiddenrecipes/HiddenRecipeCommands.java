package com.craisinlord.hiddenrecipes;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/**
 * {@code /hiddenrecipes reload|unlock|list} — debug/ops tooling for pack authors testing
 * unlock conditions without grinding them out. Registered from each loader's own command
 * registration hook (Fabric: {@code CommandRegistrationCallback}, NeoForge:
 * {@code RegisterCommandsEvent}), since Brigadier registration entrypoints differ per
 * loader even though the command tree itself is shared here.
 *
 * <p>Fixed auditing this mod: {@code unlock} previously called only
 * {@code HiddenRecipeManager#forceUnlock} and stopped there, so the recipe silently never
 * reached the player's client (JEI kept showing the hint page), never got granted to the
 * vanilla recipe book, and never toasted — despite the whole point of this command being to
 * preview a real unlock without grinding the condition out. It now runs the same
 * {@code HiddenRecipeEvents#notify} path a real condition-triggered unlock gets, same as
 * {@code retest} does elsewhere.
 */
public final class HiddenRecipeCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hiddenrecipes")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("reload")
                .executes(context -> {
                    context.getSource().sendSuccess(
                        () -> Component.literal("Hidden recipes reload via /reload (datapack reload)."), true);
                    return 1;
                }))
            .then(Commands.literal("unlock")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("recipe", ResourceLocationArgument.id())
                        .executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            ResourceLocation recipe = ResourceLocationArgument.getId(context, "recipe");
                            boolean changed = HiddenRecipeManager.INSTANCE.forceUnlock(player, recipe);
                            if (changed) {
                                // Same post-unlock path a real condition-triggered unlock gets:
                                // recipe-book grant, client sync (so JEI stops showing the hint
                                // page immediately), and the player-facing toast.
                                HiddenRecipeEvents.notify(player, Set.of(recipe));
                            }
                            context.getSource().sendSuccess(
                                () -> Component.literal((changed ? "Unlocked " : "Already unlocked: ") + recipe
                                    + " for " + player.getGameProfile().getName()),
                                true);
                            return 1;
                        }))))
            .then(Commands.literal("list")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer player = EntityArgument.getPlayer(context, "player");
                        var unlocked = HiddenRecipeManager.INSTANCE.unlockedFor(player);
                        int total = HiddenRecipeManager.INSTANCE.all().size();
                        context.getSource().sendSuccess(
                            () -> Component.literal(player.getGameProfile().getName() + ": "
                                + unlocked.size() + "/" + total + " hidden recipes unlocked"),
                            false);
                        return 1;
                    }))));
    }

    private HiddenRecipeCommands() {
    }
}
