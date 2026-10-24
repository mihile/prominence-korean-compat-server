package local.prominence.serverupdate;

import java.io.*;
import java.net.URI;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public record Release(long build, String sha256, long size, URI asset, byte[] envelope) {
    public static final String REPO = "mihile/prominence-korean-compat-server";
    public static final String FILE = "prominence-korean-compat-server-1.0.0.jar";
    public static final String LEGACY = "prominence-korean-compat-1.0.0.jar";
    public static final String STATE = ".prominence-server-update";
    public static final String IDENTITY = "assets/prominence_korean_compat/server-update-build.properties";
    public static final URI FEED = URI.create("https://github.com/" + REPO + "/releases/latest/download/server-update.properties");
    public static final long MAX_JAR = 64L * 1024 * 1024;
    public static PublicKey publicKey() throws Exception {
        try (InputStream in = Release.class.getResourceAsStream("/assets/prominence_korean_compat/server-update-public-key.der")) {
            if (in == null) throw new IOException("Missing server public key");
            return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(in.readAllBytes()));
        }
    }
    public static Release verify(byte[] envelope, PublicKey key) throws Exception {
        if (envelope.length > 128 * 1024) throw new IOException("Manifest too large");
        Properties outer = FilesEx.properties(envelope);
        byte[] payload = Base64.getDecoder().decode(FilesEx.required(outer, "payload"));
        byte[] signature = Base64.getDecoder().decode(FilesEx.required(outer, "signature"));
        Signature v = Signature.getInstance("Ed25519"); v.initVerify(key); v.update(payload);
        if (!v.verify(signature)) throw new SignatureException("Invalid server update signature");
        Properties p = FilesEx.properties(payload);
        for (var entry : Map.of("schema", "1", "channel", "server", "mod", "prominence_korean_compat",
                "pack", "4.1.3", "minecraft", "1.20.1", "version", "1.0.0", "filename", FILE).entrySet()) {
            if (!entry.getValue().equals(p.getProperty(entry.getKey()))) throw new IOException("Incompatible " + entry.getKey());
        }
        long build = Long.parseLong(FilesEx.required(p, "build"));
        long size = Long.parseLong(FilesEx.required(p, "size"));
        String hash = FilesEx.digest(p, "sha256"), tag = FilesEx.required(p, "tag");
        if (build < 1 || size < 1024 || size > MAX_JAR || !tag.matches("[A-Za-z0-9._-]{1,80}"))
            throw new IOException("Invalid release bounds");
        URI uri = URI.create(FilesEx.required(p, "url"));
        if (!uri.equals(URI.create("https://github.com/" + REPO + "/releases/download/" + tag + "/" + FILE)))
            throw new IOException("Unexpected server asset");
        return new Release(build, hash, size, uri, envelope.clone());
    }
}
