"""Real Mixin/ASM plus native/transformed slot filter differential checks, no game."""
from pathlib import Path
import argparse, json, subprocess
ROOT = Path(__file__).resolve().parents[1]
MAIN = r'''    public static void main(String[] args) throws Throwable {
        Path configs=Path.of(args[0]);Files.createDirectories(configs);
        List<URL> urls=new ArrayList<>();urls.add(configs.toUri().toURL());
        for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        LOADER=new URLClassLoader(urls.toArray(URL[]::new),VerifyMixinTransforms.class.getClassLoader());
        String config;try(InputStream in=LOADER.getResourceAsStream("prominence-trinket-slot-normalization.mixins.json")){config=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        config=config.replace("local.prominence.trinketslot.SlotNameGuard",OfflinePlugin.class.getName());
        Files.writeString(configs.resolve("offline-slot.mixins.json"),config);
        String sparkConfig;try(InputStream in=LOADER.getResourceAsStream("prominence-spark-background.mixins.json")){sparkConfig=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        sparkConfig=sparkConfig.replace("local.prominence.sparkbackground.SparkBackgroundGuard",OfflinePlugin.class.getName());
        Files.writeString(configs.resolve("offline-spark.mixins.json"),sparkConfig);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);
        com.llamalad7.mixinextras.MixinExtrasBootstrap.init();
        phase.invoke(null,MixinEnvironment.Phase.INIT);Mixins.addConfiguration("offline-slot.mixins.json");Mixins.addConfiguration("offline-spark.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);
        String target="dev.emi.trinkets.compat.WrappingTrinketsUtils";
        byte[] raw;try(InputStream in=LOADER.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
        String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(raw));
        if(!hash.equals("c7a652ff05bbe155e3ff0be5df718261e60d17382f0fddcc9dc8cdea50c3625e"))throw new AssertionError("unexpected native class");
        byte[] changed=TRANSFORMER.transformClassBytes(target,target,raw);
        if(Arrays.equals(raw,changed)||OfflinePlugin.applied!=1)throw new AssertionError("Mixin not applied once");
        ClassNode cn=new ClassNode();new ClassReader(changed).accept(cn,0);int helpers=0,redirects=0;
        for(MethodNode m:cn.methods){
            new Analyzer<BasicValue>(new BasicVerifier()).analyze(cn.name,m);
            for(AbstractInsnNode i=m.instructions.getFirst();i!=null;i=i.getNext())if(i instanceof MethodInsnNode n){
                if(n.owner.equals("local/prominence/trinketslot/SlotNameNormalizer")&&n.name.equals("normalize"))helpers++;
                if(m.name.equals("filterGroupInfo")&&n.owner.equals(cn.name)&&n.name.contains("reuseSlotNamePattern"))redirects++;
            }
        }
        if(helpers!=1||redirects!=1)throw new AssertionError("incorrect helper/redirect counts");
        SlotNativeChecks.run(raw,changed);
        System.out.println("PASS: native Tclayer Mixin transform and all native target methods ASM verified");
        target="me.lucko.spark.common.sampler.BackgroundSamplerManager";
        try(InputStream in=LOADER.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
        changed=TRANSFORMER.transformClassBytes(target,target,raw);
        if(Arrays.equals(raw,changed)||OfflinePlugin.applied!=2)throw new AssertionError("spark Mixin not applied once");
        cn=new ClassNode();new ClassReader(changed).accept(cn,0);
        for(MethodNode m:cn.methods)new Analyzer<BasicValue>(new BasicVerifier()).analyze(cn.name,m);
        SparkNativeChecks.run(raw,changed);
        System.out.println("PASS: native spark background Mixin transform and all target methods ASM verified");
    }
}
'''


def main():
    parser = argparse.ArgumentParser()
    for name in ['jar', 'classpath', 'java-home', 'output', 'mods']:
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    out = args.output.resolve()
    classes = out / 'classes'
    services = classes / 'META-INF/services'
    services.mkdir(parents=True, exist_ok=True)
    for interface, implementation in [('IMixinService', 'verification.VerifyMixinTransforms'),
                                       ('IMixinServiceBootstrap', 'verification.VerifyMixinTransforms$Bootstrap'),
                                       ('IGlobalPropertyService', 'verification.VerifyMixinTransforms$Props')]:
        (services / ('org.spongepowered.asm.service.' + interface)).write_text(implementation + '\n')
    template = (ROOT / 'tests/arrow/verification/VerifyMixinTransforms.java').read_text('utf8')
    java = out / 'src/verification/VerifyMixinTransforms.java'
    java.parent.mkdir(parents=True, exist_ok=True)
    java.write_text(template[:template.index('    public static void main(')] + MAIN, 'utf8')
    stubs = {
        'me/lucko/spark/common/SparkPlatform.java': 'package me.lucko.spark.common; public class SparkPlatform { private final SparkPlugin plugin; public SparkPlatform(SparkPlugin plugin){this.plugin=plugin;} public SparkPlugin getPlugin(){return plugin;} }',
        'me/lucko/spark/common/SparkPlugin.java': 'package me.lucko.spark.common; public interface SparkPlugin { me.lucko.spark.common.platform.PlatformInfo getPlatformInfo(); }',
        'me/lucko/spark/common/platform/PlatformInfo.java': 'package me.lucko.spark.common.platform; public interface PlatformInfo { enum Type {SERVER,CLIENT,PROXY} Type getType(); }',
        'me/lucko/spark/common/util/Configuration.java': 'package me.lucko.spark.common.util; public class Configuration { public final boolean configured; public int calls; public Configuration(boolean configured){this.configured=configured;} public boolean getBoolean(String key,boolean fallback){calls++;if(!key.equals("backgroundProfiler"))throw new AssertionError(key);return configured;} }'
    }
    stub_sources = []
    for name, content in stubs.items():
        file = out / 'stubs' / name
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(content, 'utf8')
        stub_sources.append(str(file))
    cp = ';'.join([str(classes), str(args.jar.resolve()), *[str(p.resolve()) for p in args.mods.glob('*.jar')],
                   *json.loads(args.classpath.read_text('utf8'))])
    options = ['--release', '17', '-proc:none', '-classpath', cp, '-d', str(classes), str(java),
               str(ROOT / 'tests/trinket/verification/SlotNativeChecks.java'),
               str(ROOT / 'tests/trinket/verification/SparkNativeChecks.java'), *stub_sources]
    f = out / 'compile.args'
    f.write_text('\n'.join('"' + p.replace('\\', '/') + '"' for p in options), 'utf8')
    subprocess.run([str(args.java_home / 'bin/javac.exe'), '-J-Xmx256m', '@' + str(f)], check=True)
    f = out / 'runtime.args'
    f.write_text('\n'.join('"' + p.replace('\\', '/') + '"' for p in
                           ['-Xmx256m', '-Xverify:all', '-cp', cp, 'verification.VerifyMixinTransforms', str(out / 'resources')]), 'utf8')
    result = subprocess.run([str(args.java_home / 'bin/java.exe'), '@' + str(f)], cwd=out,
                            capture_output=True, text=True, encoding='utf8', errors='replace')
    (out / 'verification.log').write_text(result.stdout + result.stderr, 'utf8')
    assert result.returncode == 0, result.stdout + result.stderr
    print(result.stdout)


if __name__ == '__main__':
    main()
