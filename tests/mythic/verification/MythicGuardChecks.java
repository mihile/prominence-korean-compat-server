package verification;
import java.nio.file.*;
import java.util.*;
import local.prominence.mythicrefresh.RefreshGuard;
import net.fabricmc.loader.api.*;
import net.fabricmc.loader.api.metadata.ModMetadata;

public final class MythicGuardChecks {
    static int checks;
    record Mod(String version,Path root)implements ModContainer {
        public ModMetadata getMetadata(){return ()->()->version;}
        public Optional<Path> findPath(String path){Path file=root.resolve(path);return Files.exists(file)?Optional.of(file):Optional.empty();}
    }
    static void expect(boolean expected){var guard=new RefreshGuard();guard.onLoad("local.prominence.mythicrefresh.mixin");if(guard.shouldApplyMixin("target","marker")!=expected)throw new AssertionError("guard check "+checks);checks++;}
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]);Map<String,ModContainer> mods=new HashMap<>();
        mods.put("minecraft",new Mod("1.20.1",root));mods.put("mythicupgrades",new Mod("4.2.0+mc1.20.1",root));FabricLoader.Holder.INSTANCE=id->Optional.ofNullable(mods.get(id));
        System.clearProperty("prominence.mythic.configRefresh");expect(true);
        System.setProperty("prominence.mythic.configRefresh","false");expect(false);System.clearProperty("prominence.mythic.configRefresh");
        for(String id:new String[]{"minecraft","mythicupgrades"}){var old=mods.remove(id);expect(false);mods.put(id,new Mod("unknown",root));expect(false);mods.put(id,old);}
        List<Path> pins;try(var files=Files.walk(root)){pins=files.filter(Files::isRegularFile).toList();}if(pins.size()!=4)throw new AssertionError("four reviewed class pins required");
        for(Path file:pins){byte[] bytes=Files.readAllBytes(file);try{byte[] bad=bytes.clone();bad[0]^=1;Files.write(file,bad);expect(false);}finally{Files.write(file,bytes);}
            Path moved=file.resolveSibling(file.getFileName()+".hidden");Files.move(file,moved);try{expect(false);}finally{Files.move(moved,file);}}
        expect(true);FabricLoader.Holder.INSTANCE=id->{throw new NoClassDefFoundError("fixture");};expect(false);
        System.out.println("PASS: "+checks+" production Mythic guard version/hash/missing/linkage/opt-out checks");
    }
}
