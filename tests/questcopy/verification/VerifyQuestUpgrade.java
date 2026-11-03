package verification;

import java.nio.file.*;
import java.util.*;
import local.prominence.serverupdate.*;

/** Real signed cumulative artifacts in a small offline filesystem fixture only. */
public final class VerifyQuestUpgrade {
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]).toAbsolutePath();
        if(Files.exists(root))throw new IllegalArgumentException("Use a fresh fixture directory");
        Files.createDirectories(root.resolve("mods"));
        Path old=Path.of(args[1]),next=Path.of(args[2]),manifest=Path.of(args[3]);
        byte[] original=Files.readAllBytes(old),expected=Files.readAllBytes(next),envelope=Files.readAllBytes(manifest);
        Release release=Release.verify(envelope,Release.publicKey());UpdateEngine.verifyJar(next,release);
        Path installed=root.resolve("mods").resolve(Release.FILE);Files.write(installed,original);
        int[] downloads={0};
        var source=new UpdateEngine.Source(){
            public byte[] manifest(){return envelope;}
            public void download(Release value,Path target)throws Exception{downloads[0]++;Files.copy(next,target,StandardCopyOption.REPLACE_EXISTING);}
        };
        var engine=new UpdateEngine(root,Release.publicKey(),source);
        for(int i=0;i<3;i++){
            engine.prepareOnly();
            if(!Arrays.equals(Files.readAllBytes(installed),expected))throw new AssertionError("new artifact not installed");
            var pending=new Properties();try(var in=Files.newInputStream(root.resolve(Release.STATE).resolve("pending.properties"))){pending.load(in);}
            if(!"true".equals(pending.getProperty("ready"))||pending.containsKey("attempted"))throw new AssertionError("prepare armed an actual startup");
        }
        if(downloads[0]!=1)throw new AssertionError("repeated preparation downloaded again");
        boolean backup=false;try(var files=Files.walk(root.resolve(Release.STATE).resolve("backups"))){for(Path p:files.filter(Files::isRegularFile).toList())if(Arrays.equals(Files.readAllBytes(p),original))backup=true;}
        if(!backup)throw new AssertionError("old cumulative artifact backup missing");
        System.out.println("PASS: real signed old-to-new artifact, preserved old backup, three preparations, one download, no startup attempt; no server/world launched");
    }
}
