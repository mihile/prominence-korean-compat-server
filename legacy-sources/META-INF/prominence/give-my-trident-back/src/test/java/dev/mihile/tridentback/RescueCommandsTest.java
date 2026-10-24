package dev.mihile.tridentback;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;

public class RescueCommandsTest {
    private CommandSourceStack source(int permission) {
        return new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, permission,
                "test", Component.literal("test"), null, null);
    }
    @Test public void actualCommandTreeParsesAndRequiresOperator() {
        var dispatcher = new CommandDispatcher<CommandSourceStack>(); RescueCommands.register(dispatcher);
        for (String command : new String[]{"gmtb", "gmtb status", "gmtb enabled false", "gmtb loyalty true", "gmtb depth 48", "gmtb reload"}) {
            var parsed = dispatcher.parse(command, source(2));
            assertFalse(command, parsed.getReader().canRead()); assertTrue(parsed.getExceptions().isEmpty());
        }
        assertTrue(dispatcher.parse("gmtb enabled true", source(0)).getReader().canRead());
        assertTrue(dispatcher.parse("gmtb depth 64", source(2)).getReader().canRead());
    }
}
