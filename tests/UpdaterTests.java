import local.prominence.serverupdate.*;
import java.io.*;
import java.net.URI;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Standalone filesystem/signature/failure tests. Never loads Minecraft or opens a server/world. */
public final class UpdaterTests {
    static final KeyPair KEY;
    static { try { KEY=KeyPairGenerator.getInstance("Ed25519").generateKeyPair(); } catch(Exception e) { throw new ExceptionInInitializerError(e); } }
    static int checks;
    static final long BUILD=2026102501L;
    static void check(boolean b,String why) { checks++; if(!b)throw new AssertionError(why); }
    interface Throwing { void run() throws Exception; }
    static void refused(Throwing r,String why) throws Exception {
        try { r.run(); } catch(Exception e) { checks++; return; } throw new AssertionError("Accepted: "+why);
    }
    static Path root() throws Exception {
        Path p=Files.createTempDirectory("prominence-server-updater-test-").toRealPath();Files.createDirectories(p.resolve("mods"));return p;
    }
    static Properties props(String... pairs) {
        Properties p=new Properties();for(int i=0;i<pairs.length;i+=2)p.setProperty(pairs[i],pairs[i+1]);return p;
    }
    static byte[] bytes(Properties p) throws Exception {
        StringWriter w=new StringWriter();p.store(w,"");return w.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    static byte[] jar(long build,boolean client) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(ZipOutputStream z=new ZipOutputStream(out)) {
            String meta="{\"id\":\"prominence_korean_compat\",\"version\":\"1.0.0\",\"environment\":\""+(build==0?"*":client?"client":"server")+"\"}";
            put(z,"fabric.mod.json",meta.getBytes());
            byte[] padding=new byte[4096];new Random(build).nextBytes(padding);put(z,"padding.bin",padding);
            if(build>0) {
                put(z,Release.IDENTITY,bytes(props("build",""+build,"channel",client?"client":"server","pack","4.1.3","minecraft","1.20.1","filename",Release.FILE)));
                put(z,"local/prominence/serverupdate/ServerHealth.class",new byte[]{1});
            }
        }
        return out.toByteArray();
    }
    static void put(ZipOutputStream z,String n,byte[] b) throws Exception {z.putNextEntry(new ZipEntry(n));z.write(b);z.closeEntry();}
    static byte[] envelope(byte[] jar,long build,String... overrides) throws Exception {
        Properties p=props("schema","1","channel","server","mod","prominence_korean_compat","pack","4.1.3","minecraft","1.20.1","version","1.0.0","filename",Release.FILE,"build",""+build,"size",""+jar.length,
            "sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(jar)),"tag","build-test.1","url","https://github.com/"+Release.REPO+"/releases/download/build-test.1/"+Release.FILE);
        for(int i=0;i<overrides.length;i+=2)p.setProperty(overrides[i],overrides[i+1]);
        byte[] payload=bytes(p);Signature s=Signature.getInstance("Ed25519");s.initSign(KEY.getPrivate());s.update(payload);
        return bytes(props("payload",Base64.getEncoder().encodeToString(payload),"signature",Base64.getEncoder().encodeToString(s.sign())));
    }
    static class Source implements UpdateEngine.Source {
        byte[] jar,manifest;int checks,downloads;boolean offline,badDownload;
        Source(long b)throws Exception{jar=jar(b,false);manifest=envelope(jar,b);}
        public byte[] manifest()throws Exception{checks++;if(offline)throw new IOException("Simulated offline");return manifest;}
        public void download(Release r,Path target)throws Exception{downloads++;Files.write(target,badDownload?new byte[]{1,2,3}:jar);}
    }
    static Path setup(Path r,long build)throws Exception {
        Path p=r.resolve("mods/"+(build==0?Release.LEGACY:Release.FILE));Files.write(p,jar(build,false));return p;
    }
    static UpdateEngine engine(Path r,Source s)throws Exception{return new UpdateEngine(r,KEY.getPublic(),s);}
    static Path state(Path r,String file){return r.resolve(Release.STATE+"/"+file);}
    static Properties read(Path p)throws Exception{return FilesEx.properties(Files.readAllBytes(p));}
    static void ack(Path r,long build)throws Exception {UpdateEngine.markHealthy(r,read(state(r,"pending.properties")).getProperty("token"),build);}
    static void bundle(Path r,Source s)throws Exception{Files.createDirectories(state(r,""));Files.write(state(r,"bootstrap.jar"),s.jar);Files.write(state(r,"bootstrap.properties"),s.manifest);}
    static void baselineUnchanged(Path r,byte[] old)throws Exception {
        check(Arrays.equals(Files.readAllBytes(r.resolve("mods/"+Release.LEGACY)),old),"legacy preserved");
        check(!Files.exists(r.resolve("mods/"+Release.FILE)),"no duplicate canonical");
    }
    static void updateAndCommit()throws Exception {
        Path r=root();Path old=setup(r,0);String hash=FilesEx.hash(old);Source s=new Source(BUILD);engine(r,s).beforeLaunch();
        Path target=r.resolve("mods/"+Release.FILE);
        check(FilesEx.hash(target).equals(Release.verify(s.manifest,KEY.getPublic()).sha256()),"updated hash");
        check(!Files.exists(old),"legacy removed");check(FilesEx.hash(state(r,"backups/"+hash+".jar")).equals(hash),"verified backup");
        refused(()->UpdateEngine.markHealthy(r,"wrong-token",BUILD),"stale receipt");
        refused(()->UpdateEngine.markHealthy(r,read(state(r,"pending.properties")).getProperty("token"),BUILD-1),"old running build");
        ack(r,BUILD);engine(r,s).beforeLaunch();
        check(!Files.exists(state(r,"pending.properties")),"journal finalized");check(read(state(r,"ledger.properties")).getProperty("highestBuild").equals(""+BUILD),"high water");
        for(int i=0;i<3;i++)engine(r,s).beforeLaunch();check(s.downloads==1,"same release not reinstalled across launches");
        // A newer signed manifest with the same old binary is rejected by embedded build, never rebooted.
        s.manifest=envelope(s.jar,BUILD+1);engine(r,s).beforeLaunch();check(!Files.exists(state(r,"pending.properties")),"manifest/binary build mismatch rejected");
    }
    static void rollback(boolean migration)throws Exception {
        Path r=root();Path old=setup(r,migration?0:BUILD-1);byte[] original=Files.readAllBytes(old);Source s=new Source(BUILD);
        engine(r,s).beforeLaunch();engine(r,s).beforeLaunch();
        check(Arrays.equals(Files.readAllBytes(old),original),"failed boot rollback exact bytes");
        check(read(state(r,"ledger.properties")).containsKey("blocked."+BUILD),"failed build blocked");
        for(int i=0;i<3;i++)engine(r,s).beforeLaunch();check(s.downloads==1,"blocked build cannot loop");
        Source next=new Source(BUILD+1);engine(r,next).beforeLaunch();check(next.downloads==1,"newer fixed build allowed");ack(r,BUILD+1);engine(r,next).beforeLaunch();
    }
    static class PowerLoss extends Error {}
    static void interruptedTransactions()throws Exception {
        for(boolean migration:new boolean[]{true,false})for(String phase:List.of("backup","journal","replace","removeLegacy")) {
            Path r=root();Path old=setup(r,migration?0:BUILD-1);byte[] original=Files.readAllBytes(old);Source s=new Source(BUILD);
            try {new UpdateEngine(r,KEY.getPublic(),s,p->{if(p.equals(phase))throw new PowerLoss();}).beforeLaunch();throw new AssertionError("No fault");}catch(PowerLoss expected){}
            s.offline=true;engine(r,s).beforeLaunch();
            check(Arrays.equals(Files.readAllBytes(old),original),"power loss recovery "+migration+" "+phase);
            check(!Files.exists(state(r,"pending.properties")),"recovery journal cleared");
            check(Files.exists(r.resolve("mods/"+Release.FILE))!=Files.exists(r.resolve("mods/"+Release.LEGACY)),"exactly one JAR");
        }
        Path r=root();Path old=setup(r,0);byte[] original=Files.readAllBytes(old);Source s=new Source(BUILD);engine(r,s).beforeLaunch();
        try{new UpdateEngine(r,KEY.getPublic(),s,p->{if(p.equals("rollback"))throw new PowerLoss();}).beforeLaunch();throw new AssertionError();}catch(PowerLoss expected){}
        engine(r,s).beforeLaunch();baselineUnchanged(r,original);
    }
    static void rejectedAndOffline()throws Exception {
        for(String mode:List.of("offline","signature","hash","client","jarBuild","size")) {
            Path r=root();Path old=setup(r,0);byte[] original=Files.readAllBytes(old);Source s=new Source(BUILD);
            switch(mode) {
                case "offline"->s.offline=true;
                case "signature"->{Properties outer=FilesEx.properties(s.manifest);byte[] signature=Base64.getDecoder().decode(outer.getProperty("signature"));signature[0]^=1;outer.setProperty("signature",Base64.getEncoder().encodeToString(signature));s.manifest=bytes(outer);}
                case "hash"->s.badDownload=true;
                case "client"->{s.jar=jar(BUILD,true);s.manifest=envelope(s.jar,BUILD);}
                case "jarBuild"->s.manifest=envelope(s.jar,BUILD+1);
                case "size"->s.manifest=envelope(s.jar,BUILD,"size","1024");
            }
            engine(r,s).beforeLaunch();baselineUnchanged(r,original);check(!Files.exists(state(r,"pending.properties")),"invalid update has no journal");
            if(mode.equals("offline")){engine(r,s).beforeLaunch();check(s.checks==1,"network failure cooldown");}
        }
        Path r=root();setup(r,0);Source s=new Source(BUILD);bundle(r,s);s.offline=true;engine(r,s).beforeLaunch();
        check(Files.exists(r.resolve("mods/"+Release.FILE)),"offline signed bootstrap installation");check(s.downloads==0,"offline bootstrap no download");
        Path disabled=root();Path old=setup(disabled,0);byte[] original=Files.readAllBytes(old);Source d=new Source(BUILD);
        Files.createDirectories(state(disabled,""));FilesEx.write(state(disabled,"config.properties"),props("enabled","false"));
        engine(disabled,d).beforeLaunch();baselineUnchanged(disabled,original);check(d.checks==0,"disabled skips network");
    }
    static void safetyStops()throws Exception {
        Path r=root();setup(r,0);Source s=new Source(BUILD);engine(r,s).beforeLaunch();
        Path target=r.resolve("mods/"+Release.FILE);Files.write(target,jar(BUILD+2,false));
        refused(()->engine(r,s).beforeLaunch(),"manual replacement during transaction");check(Arrays.equals(Files.readAllBytes(target),jar(BUILD+2,false)),"manual replacement retained");
        Path missing=root();Path old=setup(missing,0);String hash=FilesEx.hash(old);Source m=new Source(BUILD);engine(missing,m).beforeLaunch();Files.delete(state(missing,"backups/"+hash+".jar"));
        refused(()->engine(missing,m).beforeLaunch(),"missing recovery backup");
        Path duplicate=root();setup(duplicate,0);setup(duplicate,BUILD-1);refused(()->engine(duplicate,s).beforeLaunch(),"duplicate names");
        Path corrupt=root();setup(corrupt,0);Files.createDirectories(state(corrupt,""));Files.writeString(state(corrupt,"pending.properties"),"");refused(()->engine(corrupt,s).beforeLaunch(),"empty pending journal");
        Path lockRoot=root();setup(lockRoot,0);UpdateEngine e=engine(lockRoot,s);
        try(FileChannel c=FileChannel.open(state(lockRoot,"update.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock l=c.lock()) {refused(e::beforeLaunch,"concurrent helper");}
        Path high=root();Path highOld=setup(high,BUILD-1);Files.createDirectories(state(high,""));FilesEx.write(state(high,"ledger.properties"),props("highestBuild",""+(BUILD+1)));
        Source h=new Source(BUILD);engine(high,h).beforeLaunch();check(h.downloads==0&&Files.exists(highOld),"replay below high water blocked");
        refused(()->FilesEx.safe(high,"../outside"),"path traversal");
    }
    static void manifestAndUrls()throws Exception {
        byte[] jar=jar(BUILD,false);check(Release.verify(envelope(jar,BUILD),KEY.getPublic()).build()==BUILD,"valid signature");
        for(String[] pair:new String[][]{{"channel","client"},{"pack","4.1.2"},{"minecraft","1.20.2"},{"filename",Release.LEGACY},{"build","0"},{"size","999999999"},{"sha256","abcd"},{"url","https://github.com/other/file.jar"},{"tag","../evil"}})
            refused(()->Release.verify(envelope(jar,BUILD,pair),KEY.getPublic()),"signed wrong "+pair[0]);
        KeyPair other=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();refused(()->Release.verify(envelope(jar,BUILD),other.getPublic()),"wrong key");
        refused(()->Release.verify(new byte[128*1024+1],KEY.getPublic()),"oversized manifest");
        for(String uri:List.of("http://github.com/x","https://evil.example/x","https://github.com.evil.example/x","https://user@github.com/x","https://github.com:444/x","https://github.com/x#fragment","file:///C:/x"))
            refused(()->Transport.trusted(URI.create(uri)),"untrusted URL");
        for(String uri:List.of("https://github.com/x","https://release-assets.githubusercontent.com/x","https://objects.githubusercontent.com/x")) {Transport.trusted(URI.create(uri));checks++;}
    }
    public static void main(String[] args)throws Exception {
        updateAndCommit();rollback(true);rollback(false);interruptedTransactions();rejectedAndOffline();safetyStops();manifestAndUrls();
        System.out.println("PASS: "+checks+" server updater checks; no Minecraft/server/world launched.");
    }
}
