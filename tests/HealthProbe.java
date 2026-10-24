import java.nio.file.*;
import local.prominence.serverupdate.*;
/** Fixture-only receipt; the real adapter waits for lifecycle+tick events. */
public final class HealthProbe {
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]);var p=FilesEx.properties(Files.readAllBytes(root.resolve(Release.STATE+"/pending.properties")));
        UpdateEngine.markHealthy(root,p.getProperty("token"),Long.parseLong(p.getProperty("build")));
    }
}
