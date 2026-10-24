import java.nio.file.*;
import local.prominence.serverupdate.*;

public final class VerifyRelease {
    public static void main(String[] args) throws Exception {
        Release r = Release.verify(Files.readAllBytes(Path.of(args[0])), Release.publicKey());
        UpdateEngine.verifyJar(Path.of(args[1]), r);
        System.out.println("PASS: signed dedicated-server build " + r.build() + ", SHA-256 " + r.sha256());
    }
}
