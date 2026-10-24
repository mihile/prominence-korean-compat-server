package local.prominence.serverupdate;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** A total deadline bounds redirects, headers and response bodies together. */
public final class Transport implements UpdateEngine.Source {
    private static final Set<String> HOSTS = Set.of("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com");
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Server update deadline"); t.setDaemon(true); return t;
    });
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER).build();
    private final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
    public static void trusted(URI uri) throws IOException {
        if (!"https".equals(uri.getScheme()) || !HOSTS.contains(uri.getHost()) || uri.getUserInfo() != null ||
                uri.getFragment() != null || (uri.getPort() != -1 && uri.getPort() != 443)) throw new IOException("Untrusted update URL");
    }
    private InputStream open(URI uri) throws Exception {
        for (int i = 0; i < 6; i++) {
            trusted(uri);
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) throw new IOException("Update deadline exceeded");
            HttpRequest req = HttpRequest.newBuilder(uri).timeout(Duration.ofNanos(remaining))
                .header("User-Agent", "Prominence-Server-Updater/1").header("Accept", "application/octet-stream").GET().build();
            var res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() == 200) {
                InputStream in = res.body();
                ScheduledFuture<?> future = TIMER.schedule(() -> { try { in.close(); } catch (IOException ignored) {} },
                    Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                return new FilterInputStream(in) {
                    @Override public void close() throws IOException { future.cancel(false); super.close(); }
                };
            }
            res.body().close();
            if (!Set.of(301,302,303,307,308).contains(res.statusCode())) throw new IOException("GitHub HTTP " + res.statusCode());
            uri = uri.resolve(res.headers().firstValue("Location").orElseThrow(() -> new IOException("Missing redirect")));
        }
        throw new IOException("Too many redirects");
    }
    @Override public byte[] manifest() throws Exception {
        try (InputStream in = open(Release.FEED)) {
            byte[] b = in.readNBytes(128 * 1024 + 1);
            if (b.length > 128 * 1024) throw new IOException("Manifest too large"); return b;
        }
    }
    @Override public void download(Release r, Path target) throws Exception {
        try (InputStream in = open(r.asset()); OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            byte[] b = new byte[32768]; long size = 0; int n;
            while ((n = in.read(b)) != -1) {
                size += n; if (size > r.size()) throw new IOException("Download exceeds signed size"); out.write(b, 0, n);
            }
            if (size != r.size()) throw new IOException("Truncated download");
        }
        FilesEx.force(target);
    }
}
