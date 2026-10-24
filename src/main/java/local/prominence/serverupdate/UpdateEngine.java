package local.prominence.serverupdate;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.PublicKey;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.*;

/** Runs only before Minecraft's JVM. No process termination, shell commands or world access. */
public final class UpdateEngine {
    public interface Source {
        byte[] manifest() throws Exception;
        void download(Release release, Path target) throws Exception;
    }
    public interface Hook { void at(String phase) throws IOException; }
    private final Path root;
    private final PublicKey key;
    private final Source source;
    private final Hook hook;
    public UpdateEngine(Path root, PublicKey key, Source source) throws IOException {
        this(root, key, source, phase -> {});
    }
    public UpdateEngine(Path root, PublicKey key, Source source, Hook hook) throws IOException {
        this.root = root.toRealPath(); this.key = key; this.source = source; this.hook = hook;
        FilesEx.directory(this.root, "mods");
        FilesEx.directory(this.root, Release.STATE);
        FilesEx.directory(this.root, Release.STATE + "/backups");
    }
    private Path path(String relative) throws IOException { return FilesEx.safe(root, relative); }
    private Path state(String file) throws IOException { return path(Release.STATE + "/" + file); }
    private Properties readState(String file) throws IOException {
        Path p = state(file); return Files.exists(p) ? FilesEx.properties(FilesEx.read(p, 128 * 1024)) : new Properties();
    }
    private static long number(Properties p, String name, long fallback) throws IOException {
        try { return Long.parseLong(p.getProperty(name, Long.toString(fallback))); }
        catch (NumberFormatException e) { throw new IOException("Invalid state: " + name, e); }
    }
    public void log(String message) throws IOException {
        String line = java.time.Instant.now() + " " + message.replace('\n', ' ').replace('\r', ' ') + System.lineSeparator();
        System.out.print("[Server patch] " + line);
        Path log = state("updater.log");
        if (Files.exists(log) && Files.size(log) > 1024 * 1024) FilesEx.replace(log, state("updater.previous.log"));
        Files.writeString(log, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    private Path installed() throws IOException {
        Path canonical = path("mods/" + Release.FILE), legacy = path("mods/" + Release.LEGACY);
        if (Files.exists(canonical) && Files.exists(legacy)) throw new IOException("Both client-named and server-named compatibility JARs exist; refusing duplicate mod IDs");
        Path p = Files.exists(canonical) ? canonical : legacy;
        if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Missing installed compatibility JAR");
        return p;
    }
    public static Properties identity(Path jar, boolean legacyAllowed) throws IOException {
        if (!Files.isRegularFile(jar, LinkOption.NOFOLLOW_LINKS) || Files.size(jar) > Release.MAX_JAR) throw new IOException("Invalid JAR file");
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Set<String> names = new HashSet<>();
            long unpacked = 0;
            for (var entries = zip.entries(); entries.hasMoreElements();) {
                ZipEntry e = entries.nextElement();
                if (!names.add(e.getName()) || names.size() > 10000 || e.getSize() < 0 || e.getSize() > Release.MAX_JAR ||
                        (unpacked += e.getSize()) > 256L * 1024 * 1024) throw new IOException("Unsafe JAR layout");
            }
            String meta = new String(zipBytes(zip, "fabric.mod.json", 128 * 1024), java.nio.charset.StandardCharsets.UTF_8);
            if (!matches(meta, "id", "prominence_korean_compat") || !matches(meta, "version", "1.0.0") ||
                    meta.matches("(?s).*\\\"client\\\"\\s*:.*") || names.contains("local/prominence/autoupdate/UpdateClient.class"))
                throw new IOException("Not the server compatibility JAR");
            ZipEntry entry = zip.getEntry(Release.IDENTITY);
            if (entry == null && legacyAllowed) return new Properties();
            if (!matches(meta, "environment", "server") || !names.contains("local/prominence/serverupdate/ServerHealth.class"))
                throw new IOException("Missing server environment or health entrypoint");
            Properties p = FilesEx.properties(zipBytes(zip, Release.IDENTITY, 8192));
            for (var e : Map.of("channel", "server", "pack", "4.1.3", "minecraft", "1.20.1", "filename", Release.FILE).entrySet())
                if (!e.getValue().equals(p.getProperty(e.getKey()))) throw new IOException("Wrong JAR " + e.getKey());
            if (number(p, "build", 0) < 1) throw new IOException("Missing server build");
            return p;
        }
    }
    private static boolean matches(String json, String key, String value) {
        return Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"" + Pattern.quote(value) + "\\\"").matcher(json).find();
    }
    private static byte[] zipBytes(ZipFile zip, String name, int limit) throws IOException {
        ZipEntry e = zip.getEntry(name);
        if (e == null || e.getSize() > limit) throw new IOException("Missing or oversized " + name);
        try (InputStream in = zip.getInputStream(e)) {
            byte[] b = in.readNBytes(limit + 1); if (b.length > limit) throw new IOException("Oversized " + name); return b;
        }
    }
    public static void verifyJar(Path jar, Release r) throws IOException {
        if (Files.size(jar) != r.size() || !FilesEx.hash(jar).equals(r.sha256())) throw new IOException("JAR SHA-256/size mismatch");
        if (number(identity(jar, false), "build", 0) != r.build()) throw new IOException("Signed build and JAR build differ");
    }
    /** Exclusive lock protects two helpers; start hook holds a separate lock for the entire server lifetime. */
    public void beforeLaunch() throws Exception {
        try (FileChannel c = FileChannel.open(state("update.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock lock = c.tryLock()) {
            if (lock == null) throw new IOException("Another updater is running");
            if (recover()) return;
            Path current = installed();
            long build = number(identity(current, current.getFileName().toString().equals(Release.LEGACY)), "build", 0);
            Properties ledger = readState("ledger.properties"), config = readState("config.properties");
            long floor = Math.max(build, number(ledger, "highestBuild", 0));
            String currentHash = FilesEx.hash(current);
            Release candidate = null;
            Path bundled = state("bootstrap.jar"), bundledManifest = state("bootstrap.properties");
            // The locally bundled, signed first installation also works without GitHub/network availability.
            if (build == 0 && Files.exists(bundled) && Files.exists(bundledManifest)) {
                try {
                    Release local = Release.verify(FilesEx.read(bundledManifest, 128 * 1024), key);
                    verifyJar(bundled, local); candidate = local;
                } catch (Exception e) { log("Bundled initial update refused; installed JAR retained: " + e.getMessage()); }
            }
            if (!"false".equalsIgnoreCase(config.getProperty("enabled", "true")) &&
                    System.currentTimeMillis() >= number(readState("network.properties"), "retryAfter", 0)) {
                try {
                    Release remote = Release.verify(source.manifest(), key);
                    if (candidate == null || remote.build() > candidate.build()) candidate = remote;
                } catch (Exception e) {
                    Properties retry = new Properties(); retry.setProperty("retryAfter", Long.toString(System.currentTimeMillis() + 300000));
                    FilesEx.write(state("network.properties"), retry);
                    log("Update check failed; keep installed JAR. " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
            if (candidate == null || candidate.build() <= floor || candidate.sha256().equals(currentHash)) {
                log("Starting installed build " + build + "; no newer eligible server update."); return;
            }
            if (ledger.containsKey("blocked." + candidate.build())) {
                log("Build " + candidate.build() + " previously failed; automatic retry blocked."); return;
            }
            Path incoming = state("incoming-" + UUID.randomUUID() + ".jar");
            try {
                if (Files.exists(bundledManifest) && Files.exists(bundled) && FilesEx.hash(bundled).equals(candidate.sha256()))
                    Files.copy(bundled, incoming);
                else source.download(candidate, incoming);
                verifyJar(incoming, candidate);
                if (!FilesEx.hash(current).equals(currentHash) || !installed().equals(current)) throw new IOException("Installed JAR changed during update");
                install(current, currentHash, incoming, candidate);
                log("Installed server build " + candidate.build() + "; awaiting successful startup plus 60 seconds of ticking.");
            } catch (Exception e) {
                if (Files.exists(state("pending.properties"))) recover();
                log("Update preparation/apply failed; previous JAR retained. " + e.getClass().getSimpleName() + ": " + e.getMessage());
            } finally { Files.deleteIfExists(incoming); }
        }
    }
    private void install(Path old, String oldHash, Path incoming, Release r) throws Exception {
        Path backup = state("backups/" + oldHash + ".jar");
        if (!Files.exists(backup)) { Files.copy(old, backup); FilesEx.force(backup); }
        if (!FilesEx.hash(backup).equals(oldHash)) throw new IOException("Backup validation failed");
        hook.at("backup");
        Properties p = new Properties();
        p.setProperty("oldFile", old.getFileName().toString()); p.setProperty("oldHash", oldHash);
        p.setProperty("newHash", r.sha256()); p.setProperty("build", Long.toString(r.build()));
        p.setProperty("token", UUID.randomUUID().toString());
        FilesEx.write(state("pending.properties"), p); hook.at("journal");
        FilesEx.replace(incoming, path("mods/" + Release.FILE)); hook.at("replace");
        if (!old.getFileName().toString().equals(Release.FILE)) {
            if (!FilesEx.hash(old).equals(oldHash)) throw new IOException("Legacy JAR changed before migration");
            Files.delete(old);
        }
        hook.at("removeLegacy");
    }
    /** @return true when rollback occurred; don't fetch/reinstall anything on that launch. */
    public boolean recover() throws Exception {
        Properties p = readState("pending.properties");
        if (p.isEmpty()) {
            if (Files.exists(state("pending.properties"))) throw new IOException("Empty recovery journal; manual review required");
            return false;
        }
        String oldName = FilesEx.required(p, "oldFile"), oldHash = FilesEx.digest(p, "oldHash"), newHash = FilesEx.digest(p, "newHash");
        if (!Set.of(Release.FILE, Release.LEGACY).contains(oldName) || !FilesEx.required(p, "token").matches("[a-f0-9-]{36}"))
            throw new IOException("Invalid recovery journal");
        long build = number(p, "build", -1); if (build < 1) throw new IOException("Invalid recovery build");
        Path target = path("mods/" + Release.FILE), old = path("mods/" + oldName);
        Properties ledger = readState("ledger.properties"), healthy = readState("healthy.properties");
        boolean targetNew = Files.exists(target) && FilesEx.hash(target).equals(newHash);
        if (targetNew && p.getProperty("token").equals(healthy.getProperty("token")) &&
                newHash.equals(healthy.getProperty("sha256")) && Long.toString(build).equals(healthy.getProperty("build")) &&
                (old.equals(target) || !Files.exists(old))) {
            ledger.setProperty("highestBuild", Long.toString(Math.max(build, number(ledger, "highestBuild", 0))));
            FilesEx.write(state("ledger.properties"), ledger);
            Files.delete(state("pending.properties")); Files.deleteIfExists(state("healthy.properties"));
            log("Confirmed healthy server build " + build + "."); return false;
        }
        // Never overwrite an administrator's unrelated manual replacement while recovering.
        if (Files.exists(target) && !targetNew && !(old.equals(target) && FilesEx.hash(target).equals(oldHash)))
            throw new IOException("Installed JAR was manually changed during pending update; recovery needs review");
        if (!old.equals(target) && Files.exists(old) && !FilesEx.hash(old).equals(oldHash))
            throw new IOException("Legacy JAR was manually changed; recovery needs review");
        Path backup = state("backups/" + oldHash + ".jar");
        if (!Files.exists(backup) || !FilesEx.hash(backup).equals(oldHash)) throw new IOException("Recovery backup is missing or damaged");
        ledger.setProperty("blocked." + build, newHash); FilesEx.write(state("ledger.properties"), ledger);
        Path restore = state("restore-" + UUID.randomUUID() + ".jar");
        try {
            Files.copy(backup, restore); FilesEx.force(restore); FilesEx.replace(restore, old);
            if (!old.equals(target) && targetNew) Files.delete(target);
            if (!FilesEx.hash(old).equals(oldHash)) throw new IOException("Restored JAR hash mismatch");
            hook.at("rollback");
            Files.delete(state("pending.properties")); Files.deleteIfExists(state("healthy.properties"));
            log("Rolled back failed/unconfirmed build " + build + "; this build is now blocked."); return true;
        } finally { Files.deleteIfExists(restore); }
    }
    /** Called by the server-only lifecycle adapter after startup and a stable tick window. */
    public static void markHealthy(Path root, String token, long runningBuild) throws Exception {
        root = root.toRealPath();
        Path pending = FilesEx.safe(root, Release.STATE + "/pending.properties");
        if (token == null || !Files.exists(pending)) return;
        Properties p = FilesEx.properties(FilesEx.read(pending, 128 * 1024));
        if (!token.equals(p.getProperty("token")) || runningBuild != number(p, "build", -1)) throw new IOException("Stale health receipt refused");
        String hash = FilesEx.digest(p, "newHash");
        if (!FilesEx.hash(FilesEx.safe(root, "mods/" + Release.FILE)).equals(hash)) throw new IOException("Running installation changed");
        Properties healthy = new Properties(); healthy.setProperty("token", token); healthy.setProperty("sha256", hash);
        healthy.setProperty("build", Long.toString(runningBuild));
        FilesEx.write(FilesEx.safe(root, Release.STATE + "/healthy.properties"), healthy);
    }
    public static void main(String[] args) {
        UpdateEngine engine = null;
        try {
            if (args.length != 1) throw new IllegalArgumentException("Expected server root only");
            engine = new UpdateEngine(Path.of(args[0]), Release.publicKey(), new Transport());
            engine.beforeLaunch();
        } catch (Exception e) {
            if (engine != null) try { engine.log("SAFETY STOP: " + e); } catch (IOException ignored) {}
            System.err.println("[Server patch] Unsafe state; server launch stopped for review: " + e);
            System.exit(20);
        }
    }
}
