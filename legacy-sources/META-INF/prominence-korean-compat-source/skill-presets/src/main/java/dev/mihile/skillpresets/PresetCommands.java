package dev.mihile.skillpresets;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.SkillsAPI;
import static net.minecraft.commands.Commands.*;

public final class PresetCommands {
    private PresetCommands() { }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("skillpreset").executes(c -> status(c.getSource()))
                .then(literal("status").executes(c -> status(c.getSource())))
                .then(literal("open").executes(c -> { SkillsAPI.openScreen(c.getSource().getPlayerOrException()); return 1; }))
                .then(literal("use").then(argument("slot", IntegerArgumentType.integer(1, 3))
                        .executes(c -> select(c.getSource(), IntegerArgumentType.getInteger(c, "slot") - 1))))
                .then(literal("name").then(argument("slot", IntegerArgumentType.integer(1, 3))
                        .then(argument("name", StringArgumentType.greedyString()).executes(c -> rename(c.getSource(),
                                IntegerArgumentType.getInteger(c, "slot") - 1, StringArgumentType.getString(c, "name")))))));
        dispatcher.register(literal("spreset").executes(c -> status(c.getSource())).redirect(dispatcher.getRoot().getChild("skillpreset")));
        // Greedy strings accept Korean and spaces, unlike Brigadier's ASCII word parser.
        dispatcher.register(literal("스킬트리").executes(c -> status(c.getSource()))
                .then(literal("이름변경").then(argument("names", StringArgumentType.greedyString()).suggests((c, builder) -> {
                    ServerPlayer player = c.getSource().getPlayer();
                    if (player != null) PresetNames.renameTargets(PresetController.data(player).presets$book(), builder.getRemaining())
                            .forEach(target -> builder.suggest(target, Component.literal("변경할 프리셋 · 뒤에 새 이름 입력")));
                    return builder.buildFuture();
                }).executes(c -> {
                    ServerPlayer player = c.getSource().getPlayerOrException();
                    try {
                        var change = PresetNames.parseRename(PresetController.data(player).presets$book(), StringArgumentType.getString(c, "names"));
                        return rename(c.getSource(), change.slot(), change.name());
                    } catch (RuntimeException failure) { return fail(c.getSource(), player, failure); }
                })))
                .then(argument("preset", StringArgumentType.greedyString()).suggests((c, builder) -> {
                    ServerPlayer player = c.getSource().getPlayer();
                    if (player != null) {
                        PresetBook book = PresetController.data(player).presets$book();
                        PresetNames.suggestions(book, builder.getRemaining()).forEach(name ->
                                builder.suggest(name, Component.literal("프리셋 선택")));
                    }
                    return builder.buildFuture();
                }).executes(c -> {
                    ServerPlayer player = c.getSource().getPlayerOrException();
                    try { return select(c.getSource(), PresetNames.resolve(PresetController.data(player).presets$book(), StringArgumentType.getString(c, "preset"))); }
                    catch (RuntimeException failure) { return fail(c.getSource(), player, failure); }
                })));
    }
    private static int select(CommandSourceStack source, int slot) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            PresetBook book = PresetController.data(player).presets$book();
            int before = book.active();
            PresetController.select(player, slot);
            PresetFeedback.confirm(player, PresetFeedback.selection(book, before));
            PresetNetwork.sync(player);
            try { SkillsAPI.openScreen(player); }
            catch (RuntimeException failure) { SkillPresets.LOGGER.warn("Preset applied; could not open skill screen for {}", player.getUUID(), failure); }
            return 1;
        } catch (RuntimeException failure) { return fail(source, player, failure); }
    }
    private static int rename(CommandSourceStack source, int slot, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            PresetBook book = PresetController.data(player).presets$book();
            String before = PresetNames.display(book, slot);
            book.rename(slot, name);
            PresetNetwork.sync(player);
            PresetFeedback.confirm(player, PresetFeedback.renamed(slot, before, book.name(slot)));
            return 1;
        } catch (RuntimeException failure) { return fail(source, player, failure); }
    }
    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            if (PresetNetwork.hasUi(player)) { PresetNetwork.sync(player); SkillsAPI.openScreen(player); return 1; }
            PresetBook book = PresetController.data(player).presets$book();
            MutableComponent menu = PresetFeedback.prefix();
            for (int i = 0; i < 3; i++) {
                final int slot = i;
                menu.append(Component.literal("[" + PresetNames.display(book, i) + "] ").withStyle(style -> style
                        .withColor(slot == book.active() ? ChatFormatting.GREEN : ChatFormatting.AQUA)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/skillpreset use " + (slot + 1)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("선택 " + (slot + 1))))));
            }
            source.sendSuccess(() -> menu, false); return 1;
        } catch (RuntimeException failure) { return fail(source, player, failure); }
    }
    private static int fail(CommandSourceStack source, ServerPlayer player, RuntimeException failure) {
        String reason = failure.getMessage() == null ? "처리 실패" : failure.getMessage();
        if (PresetNetwork.hasUi(player)) PresetNetwork.error(player, reason);
        else source.sendFailure(PresetFeedback.prefix().append(Component.literal(reason).withStyle(ChatFormatting.RED)));
        SkillPresets.LOGGER.debug("Preset command failed for {}", player.getUUID(), failure);
        return 0;
    }
}
