package dev.mihile.tridentback;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class RescueConfigTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void defaultsProtectAllPlayerTridentsAndTriggerEarly() {
        var config = new RescueConfig(); config.validate();
        assertTrue(config.enabled); assertFalse(config.requireLoyalty); assertEquals(8, config.voidDepth);
    }
    @Test public void editsAndDiskRoundTripPreserveCurrentConfigUntilCommit() throws Exception {
        var original = new RescueConfig(); var changed = original.edited(c -> { c.voidDepth = 16; c.requireLoyalty = true; });
        var path = temporary.getRoot().toPath().resolve("config.json"); changed.write(path);
        var restored = RescueConfig.read(path);
        assertEquals(8, original.voidDepth); assertEquals(16, restored.voidDepth); assertTrue(restored.requireLoyalty);
    }
    @Test public void unsafeDepthCannotReachVanillaRemovalBoundary() {
        assertThrows(IllegalArgumentException.class, () -> new RescueConfig().edited(c -> c.voidDepth = 64));
        assertThrows(IllegalArgumentException.class, () -> new RescueConfig().edited(c -> c.voidDepth = -1));
    }
    @Test public void malformedReloadDoesNotOverwriteFile() throws Exception {
        var path = temporary.getRoot().toPath().resolve("config.json"); Files.writeString(path, "{broken");
        assertThrows(IOException.class, () -> RescueConfig.read(path)); assertEquals("{broken", Files.readString(path));
    }
}
