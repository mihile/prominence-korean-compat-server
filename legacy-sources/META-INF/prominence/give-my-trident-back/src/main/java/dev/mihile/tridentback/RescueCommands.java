package dev.mihile.tridentback;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import static net.minecraft.commands.Commands.*;

public final class RescueCommands {
    private static final DynamicCommandExceptionType ERROR = new DynamicCommandExceptionType(message -> Component.literal(String.valueOf(message)));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("gmtb").requires(source -> source.hasPermission(2))
                .executes(c -> status(c.getSource()))
                .then(literal("status").executes(c -> status(c.getSource())))
                .then(literal("enabled").then(argument("value", BoolArgumentType.bool()).executes(c ->
                        edit(c, config -> config.enabled = BoolArgumentType.getBool(c, "value"), "enabled"))))
                .then(literal("loyalty").then(argument("required", BoolArgumentType.bool()).executes(c ->
                        edit(c, config -> config.requireLoyalty = BoolArgumentType.getBool(c, "required"), "requireLoyalty"))))
                .then(literal("depth").then(argument("blocks", IntegerArgumentType.integer(0, 48)).executes(c ->
                        edit(c, config -> config.voidDepth = IntegerArgumentType.getInteger(c, "blocks"), "voidDepth"))))
                .then(literal("reload").executes(c -> {
                    RescueConfig before = GiveMyTridentBack.config();
                    try { GiveMyTridentBack.reload(); }
                    catch (Exception e) { throw ERROR.create("설정 불러오기 실패: " + e.getMessage()); }
                    CommandFeedback.changes(before, GiveMyTridentBack.config()).forEach(line -> reply(c.getSource(), line));
                    reply(c.getSource(), "설정을 다시 읽어 적용했습니다."); return 1;
                })));
    }

    private static int edit(CommandContext<CommandSourceStack> context, Consumer<RescueConfig> edit, String... requested) throws CommandSyntaxException {
        RescueConfig before = GiveMyTridentBack.config();
        try { GiveMyTridentBack.update(edit); }
        catch (Exception e) { throw ERROR.create("설정 저장 실패; 기존 설정을 유지합니다: " + e.getMessage()); }
        CommandFeedback.changes(before, GiveMyTridentBack.config(), requested).forEach(line -> reply(context.getSource(), line));
        reply(context.getSource(), "즉시 적용하고 config/give-my-trident-back.json에 저장했습니다."); return 1;
    }

    private static int status(CommandSourceStack source) {
        var config = GiveMyTridentBack.config();
        reply(source, "give my trident back 설정 (true=켜짐, false=꺼짐):");
        CommandFeedback.status(config).forEach(line -> reply(source, line));
        reply(source, "구조 시작 횟수: " + RescueController.rescuedCount());
        reply(source, "삼지창 / Simply Swords 실제 투척 무기 / Soulslike Weaponry 귀환 무기 지원. 진행 중인 회수는 비활성화해도 보호합니다.");
        return 1;
    }

    private static void reply(CommandSourceStack source, String message) { source.sendSuccess(() -> Component.literal(message), false); }
}
