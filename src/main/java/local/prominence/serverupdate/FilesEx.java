package local.prominence.serverupdate;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** All mutable paths are fixed children of the chosen server root. */
public final class FilesEx {
    private FilesEx() {}
    public static Path safe(Path root, String relative) throws IOException {
        Path result = root.resolve(relative).normalize();
        if (!result.startsWith(root) || result.equals(root)) throw new IOException("Path outside server root");
        Path part = root;
        for (Path name : root.relativize(result)) {
            part = part.resolve(name);
            if (Files.exists(part, LinkOption.NOFOLLOW_LINKS) &&
                (Files.isSymbolicLink(part) || !part.toRealPath().equals(part)))
                throw new IOException("Linked path refused: " + part);
        }
        return result;
    }
    public static Path directory(Path root, String relative) throws IOException {
        Path p = safe(root, relative);
        Files.createDirectories(p);
        safe(root, relative);
        if (!Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Not a directory");
        return p;
    }
    public static byte[] read(Path p, int limit) throws IOException {
        if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS) || Files.size(p) > limit)
            throw new IOException("Invalid file: " + p);
        try (InputStream in = Files.newInputStream(p)) {
            byte[] b = in.readNBytes(limit + 1);
            if (b.length > limit) throw new IOException("File too large");
            return b;
        }
    }
    public static String hash(Path p) throws IOException {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(p)) {
                byte[] b = new byte[32768]; int n;
                while ((n = in.read(b)) != -1) d.update(b, 0, n);
            }
            return HexFormat.of().formatHex(d.digest());
        } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    public static Properties properties(byte[] b) throws IOException {
        Properties p = new Properties();
        p.load(new StringReader(new String(b, java.nio.charset.StandardCharsets.UTF_8)));
        return p;
    }
    public static void write(Path p, Properties properties) throws IOException {
        StringWriter w = new StringWriter(); properties.store(w, "Prominence server updater");
        atomic(p, w.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    public static void atomic(Path p, byte[] b) throws IOException {
        Path temp = p.resolveSibling(p.getFileName() + ".tmp-" + UUID.randomUUID());
        try {
            Files.write(temp, b, StandardOpenOption.CREATE_NEW);
            force(temp); replace(temp, p);
        } finally { Files.deleteIfExists(temp); }
    }
    public static void force(Path p) throws IOException {
        try (FileChannel c = FileChannel.open(p, StandardOpenOption.WRITE)) { c.force(true); }
    }
    public static void replace(Path from, Path to) throws IOException {
        try { Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) {
            // The durable journal and verified backup recover interrupted non-atomic moves.
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    public static String required(Properties p, String key) throws IOException {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) throw new IOException("Missing " + key);
        return v;
    }
    public static String digest(Properties p, String key) throws IOException {
        String v = required(p, key);
        if (!v.matches("[a-f0-9]{64}")) throw new IOException("Invalid " + key);
        return v;
    }
}
