package dev.mihile.tridentback;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;
import java.util.function.Consumer;

public final class RescueConfig {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public boolean enabled = true;
    public boolean requireLoyalty = false;
    public int voidDepth = 8;

    public void validate() {
        if (voidDepth < 0 || voidDepth > 48) throw new IllegalArgumentException("voidDepth must be 0..48");
    }

    public RescueConfig edited(Consumer<RescueConfig> edit) {
        RescueConfig result = GSON.fromJson(GSON.toJson(this), RescueConfig.class);
        edit.accept(result); result.validate(); return result;
    }

    public static RescueConfig read(Path path) throws IOException {
        try {
            RescueConfig config = GSON.fromJson(Files.readString(path), RescueConfig.class);
            if (config == null) throw new IllegalArgumentException("Configuration cannot be null");
            config.validate(); return config;
        } catch (RuntimeException e) { throw new IOException("Invalid configuration: " + e.getMessage(), e); }
    }

    public void write(Path path) throws IOException {
        validate();
        Path directory = path.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "give-my-trident-back-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(this) + System.lineSeparator());
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
