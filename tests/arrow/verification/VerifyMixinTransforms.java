package verification;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterConsole;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IAdviceProvider;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IFeatureValidator;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.IMixinServiceBootstrap;
import org.spongepowered.asm.service.IPropertyKey;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.ReEntranceLock;

public final class VerifyMixinTransforms
        implements IMixinService, IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker {

    public static final class Bootstrap implements IMixinServiceBootstrap {
        @Override
        public String getName() {
            return "OfflineMixinVerifier";
        }

        @Override
        public String getServiceClassName() {
            return VerifyMixinTransforms.class.getName();
        }

        @Override
        public void bootstrap() {}
    }

    public static final class Props implements IGlobalPropertyService {
        private static final Map<IPropertyKey, Object> MAP = new HashMap<>();

        private record Key(String name) implements IPropertyKey {}

        @Override
        public IPropertyKey resolveKey(String name) {
            return new Key(name);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getProperty(IPropertyKey key) {
            return (T) MAP.get(key);
        }

        @Override
        public void setProperty(IPropertyKey key, Object value) {
            MAP.put(key, value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getProperty(IPropertyKey key, T defaultValue) {
            return (T) MAP.getOrDefault(key, defaultValue);
        }

        @Override
        public String getPropertyString(IPropertyKey key, String defaultValue) {
            Object v = MAP.get(key);
            return v != null ? v.toString() : defaultValue;
        }
    }

    public static final class OfflinePlugin implements IMixinConfigPlugin {
        static int applied;
        public void onLoad(String p) {}
        public String getRefMapperConfig() { return null; }
        public boolean shouldApplyMixin(String a,String b) { return true; }
        public void acceptTargets(Set<String>a,Set<String>b) {}
        public List<String> getMixins() { return null; }
        public void preApply(String a,ClassNode b,String c,IMixinInfo d) {}
        public void postApply(String a,ClassNode b,String c,IMixinInfo d) { applied++; }
    }

    private static URLClassLoader LOADER;
    private static IMixinTransformer TRANSFORMER;
    private final ReEntranceLock lock = new ReEntranceLock(1);

    @Override
    public String getName() {
        return "OfflineMixinVerifier";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void prepare() {}

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        return MixinEnvironment.Phase.PREINIT;
    }

    @Override
    public void offer(IMixinInternal internal) {
        if (internal instanceof IMixinTransformerFactory factory && TRANSFORMER == null) {
            TRANSFORMER = factory.createTransformer();
        }
    }

    @Override
    public void init() {}

    @Override
    public void beginPhase() {}

    @Override
    public void checkEnv(Object bootSource) {}

    @Override
    public ReEntranceLock getReEntranceLock() {
        return lock;
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return this;
    }

    @Override
    public IClassTracker getClassTracker() {
        return this;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public IFeatureValidator getFeatureValidator() {
        return IFeatureValidator.ALLOW_ALL;
    }

    @Override
    public IAdviceProvider getAdviceProvider() {
        return IAdviceProvider.GENERIC;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return List.of("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault");
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return new ContainerHandleVirtual(getName());
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return Collections.emptyList();
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return LOADER.getResourceAsStream(name);
    }

    @Override
    public String getSideName() {
        return Constants.SIDE_SERVER;
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_8;
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_21;
    }

    @Override
    public ILogger getLogger(String name) {
        return new LoggerAdapterConsole(name);
    }

    @Override
    public URL[] getClassPath() {
        return LOADER.getURLs();
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name, false, LOADER);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, LOADER);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, VerifyMixinTransforms.class.getClassLoader());
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, true, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        return getClassNode(name, runTransformers, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags)
            throws ClassNotFoundException, IOException {
        String res = name.replace('.', '/') + ".class";
        try (InputStream in = LOADER.getResourceAsStream(res)) {
            if (in == null) {
                throw new ClassNotFoundException(name);
            }
            ClassNode cn = new ClassNode();
            new ClassReader(in.readAllBytes()).accept(cn, readerFlags);
            return cn;
        }
    }

    @Override
    public Collection<ITransformer> getTransformers() {
        return Collections.emptyList();
    }

    @Override
    public Collection<ITransformer> getDelegatedTransformers() {
        return Collections.emptyList();
    }

    @Override
    public void addTransformerExclusion(String name) {}

    @Override
    public void registerInvalidClass(String className) {}

    @Override
    public boolean isClassLoaded(String className) {
        return false;
    }

    @Override
    public String getClassRestrictions(String className) {
        return "";
    }

    public static void main(String[] args) throws Exception {
        Path output=Path.of(args[0]),configs=Path.of(args[1]);Files.createDirectories(configs);
        List<URL> urls=new ArrayList<>();urls.add(configs.toUri().toURL());
        for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        LOADER=new URLClassLoader(urls.toArray(URL[]::new),VerifyMixinTransforms.class.getClassLoader());
        String config;try(InputStream in=LOADER.getResourceAsStream("prominence-arrow-tick.mixins.json")){config=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        Files.writeString(configs.resolve("offline-arrow.mixins.json"),config.replace("local.prominence.arrowtick.ArrowTickGuard",OfflinePlugin.class.getName()));
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);
        phase.invoke(null,MixinEnvironment.Phase.INIT);Mixins.addConfiguration("offline-arrow.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);
        int methods=0;Map<String,Integer> calls=new HashMap<>();
        for(String target:List.of("net.spell_engine.internals.SpellHelper","com.bawnorton.neruina.util.TickingEntry")) {
            byte[] raw;try(InputStream in=LOADER.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
            byte[] transformed=TRANSFORMER.transformClassBytes(target,target,raw);if(Arrays.equals(raw,transformed))throw new AssertionError("Unchanged target "+target);
            ClassNode cn=new ClassNode();new ClassReader(transformed).accept(cn,0);
            for(MethodNode m:cn.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(cn.name,m);methods++;
                for(AbstractInsnNode i=m.instructions.getFirst();i!=null;i=i.getNext())if(i instanceof MethodInsnNode call&&call.owner.startsWith("local/prominence/arrowtick/"))calls.merge(call.name,1,Integer::sum);
            }
            Path dest=output.resolve(target.replace('.','/')+".class");Files.createDirectories(dest.getParent());Files.write(dest,transformed);
        }
        if(OfflinePlugin.applied!=2||calls.getOrDefault("complete",0)!=1||calls.getOrDefault("read",0)!=1||calls.getOrDefault("write",0)!=1)throw new AssertionError("Missing injection "+calls);
        int guards=0;
        for(String mod:List.of("spell_engine","spell_power","neruina")) {
            var pins=local.prominence.arrowtick.ArrowTickGuard.pins(mod);Map<String,byte[]> originals=new HashMap<>();
            for(String name:pins.keySet())try(InputStream in=LOADER.getResourceAsStream(name)){originals.put(name,in.readAllBytes());}
            if(!local.prominence.arrowtick.ArrowTickGuard.matches(pins,originals))throw new AssertionError("Original rejected "+mod);guards++;
            for(String name:pins.keySet()){
                byte[] raw=originals.get(name),bad=raw.clone();bad[bad.length-1]^=1;originals.put(name,bad);
                if(local.prominence.arrowtick.ArrowTickGuard.matches(pins,originals))throw new AssertionError("Changed class accepted");guards++;
                originals.remove(name);if(local.prominence.arrowtick.ArrowTickGuard.matches(pins,originals))throw new AssertionError("Absent class accepted");guards++;originals.put(name,raw);
            }
        }
        System.out.println("PASS: two original targets / "+methods+" methods transformed and ASM verified; "+guards+" fingerprint checks; "+calls);
    }
}
