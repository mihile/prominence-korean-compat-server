"""Real native Mixin transformation and full refresh-body fixture, no Minecraft world."""
from pathlib import Path
import argparse, json, os, subprocess
ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser()
    for name in ['jar', 'classpath', 'java-home', 'mods', 'output']:
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--transform-only', action='store_true')
    args = parser.parse_args()
    work = args.output.resolve(); classes = work / 'transform-classes'; classes.mkdir(parents=True, exist_ok=True)
    cp = [str(args.jar.resolve()), *[str(p.resolve()) for p in args.mods.glob('*.jar')], *json.loads(args.classpath.read_text('utf8'))]
    java, javac = [args.java_home / ('bin/' + name + '.exe') for name in ['java', 'javac']]

    def run(executable, options, label):
        file = work / (label + '.args')
        jvm = [str(s) for s in options if str(s).startswith('-J')]
        file.write_text('\n'.join('"' + str(s).replace('\\', '/') + '"' for s in options if not str(s).startswith('-J')), 'utf8')
        result = subprocess.run([str(executable), *jvm, '@' + str(file)], capture_output=True, text=True, encoding='utf8', errors='replace')
        (work / (label + '.log')).write_text(result.stdout + result.stderr, 'utf8')
        assert result.returncode == 0, result.stdout + result.stderr
        print(result.stdout.strip())

    sources = [ROOT / 'tests/arrow/verification/VerifyMixinTransforms.java', ROOT / 'tests/mythic/verification/VerifyMythicTransform.java']
    run(javac, ['-J-Xmx256m', '--release', '17', '-encoding', 'UTF-8', '-proc:none', '-cp', os.pathsep.join(cp), '-d', classes, *sources], 'compile-transform')
    services = classes / 'META-INF/services'; services.mkdir(parents=True, exist_ok=True)
    for interface, implementation in [('IMixinService', 'verification.VerifyMixinTransforms'),
                                       ('IMixinServiceBootstrap', 'verification.VerifyMixinTransforms$Bootstrap'),
                                       ('IGlobalPropertyService', 'verification.VerifyMixinTransforms$Props')]:
        (services / ('org.spongepowered.asm.service.' + interface)).write_text(implementation + '\n', 'utf8')
    run(java, ['-Xmx384m', '-Xverify:all', '-cp', os.pathsep.join([str(classes), *cp]), 'verification.VerifyMythicTransform', work], 'transform')
    if args.transform_only:
        return
    run_fixture(work, cp, run, java, javac)


def run_fixture(work, cp, run, java, javac):
    model = json.loads((work / 'fixture-model.json').read_text('utf8'))
    fixture = work / 'fixture-classes'; fixture.mkdir(parents=True, exist_ok=True)
    stubs = {
        'net/minecraft/server/MinecraftServer.java': 'package net.minecraft.server; public class MinecraftServer {public int ticks;public boolean onThread=true;public int method_3780(){return ticks;}public boolean method_18854(){return onThread;}}',
        'net/minecraft/class_3218.java': 'package net.minecraft; public class class_3218 {private final net.minecraft.server.MinecraftServer server;public class_3218(net.minecraft.server.MinecraftServer server){this.server=server;}public net.minecraft.server.MinecraftServer method_8503(){return server;}}',
        'net/minecraft/class_1291.java': 'package net.minecraft; public class class_1291 {private final String id;public class_1291(String id){this.id=id;}public String toString(){return id;}}'
    }
    stubs.update({
        'net/fabricmc/loader/api/Version.java': 'package net.fabricmc.loader.api;public interface Version {String getFriendlyString();}',
        'net/fabricmc/loader/api/metadata/ModMetadata.java': 'package net.fabricmc.loader.api.metadata;public interface ModMetadata {net.fabricmc.loader.api.Version getVersion();}',
        'net/fabricmc/loader/api/ModContainer.java': 'package net.fabricmc.loader.api;public interface ModContainer {net.fabricmc.loader.api.metadata.ModMetadata getMetadata();java.util.Optional<java.nio.file.Path> findPath(String path);}',
        'net/fabricmc/loader/api/FabricLoader.java': 'package net.fabricmc.loader.api;public interface FabricLoader {static FabricLoader getInstance(){return Holder.INSTANCE;}java.util.Optional<ModContainer> getModContainer(String id);class Holder {public static FabricLoader INSTANCE=id->java.util.Optional.empty();}}'
    })
    groups = [f for f in model['fields'] if f['owner'] == 'net/trique/mythicupgrades/config/MUConfig']
    getters = [m for m in model['calls'] if m['owner'].startswith('net/trique/mythicupgrades/config/MUConfig$')]
    source = 'package net.trique.mythicupgrades.config;public class MUConfig {'
    for field in groups:
        nested = field['desc'].split('$')[1].rstrip(';')
        source += 'public final ' + nested + ' ' + field['name'] + '=new ' + nested + '();'
        source += 'public static class ' + nested + ' {'
        for getter in getters:
            if getter['owner'] != field['desc'][1:-1]:
                continue
            return_type, accessor = ('int', 'intValue') if getter['desc'] == '()I' else ('float', 'floatValue')
            source += 'public ' + return_type + ' ' + getter['name'] + '(){return verification.MythicFixtureState.value("' + getter['owner'] + '.' + getter['name'] + '").' + accessor + '();}'
        source += '}'
    stubs['net/trique/mythicupgrades/config/MUConfig.java'] = source + '}'
    stubs['net/trique/mythicupgrades/MythicUpgrades.java'] = 'package net.trique.mythicupgrades;public class MythicUpgrades {public static final net.trique.mythicupgrades.config.MUConfig CONFIG=new net.trique.mythicupgrades.config.MUConfig();}'
    for owner in ['net/trique/mythicupgrades/effect/MUEffects', 'net/minecraft/class_1294']:
        package, name = owner.rsplit('/', 1)
        source = 'package ' + package.replace('/', '.') + ';public class ' + name + ' {'
        for field in model['fields']:
            if field['owner'] == owner:
                assert field['desc'] == 'Lnet/minecraft/class_1291;'
                source += 'public static final net.minecraft.class_1291 ' + field['name'] + '=new net.minecraft.class_1291("' + field['name'] + '");'
        stubs[owner + '.java'] = source + '}'
    types = dict(model['parents'])
    for method in model['calls']:
        if method['owner'].startswith('net/trique/mythicupgrades/item/'):
            types.setdefault(method['owner'], 'verification/MythicFixtureState$Item')
    for owner, parent in types.items():
        package, name = owner.rsplit('/', 1)
        parent = parent.replace('/', '.') if parent in types else 'verification.MythicFixtureState.Item'
        stubs[owner + '.java'] = 'package ' + package.replace('/', '.') + ';public class ' + name + ' extends ' + parent + ' {public ' + name + '(String id){super(id);}}'
    source = 'package net.trique.mythicupgrades.item;public class MUItems {'
    for field in model['fields']:
        if field['owner'] == 'net/trique/mythicupgrades/item/MUItems':
            kind = field['desc'][1:-1].replace('/', '.')
            source += 'public static final ' + kind + ' ' + field['name'] + '=new ' + kind + '("' + field['name'] + '");'
    stubs['net/trique/mythicupgrades/item/MUItems.java'] = source + '}'
    sources = []
    for name, content in stubs.items():
        file = work / 'stubs' / name; file.parent.mkdir(parents=True, exist_ok=True); file.write_text(content, 'utf8'); sources.append(file)
    sources.extend(ROOT / ('tests/mythic/verification/' + name + '.java') for name in
                   ['PrepareMythicFixture', 'MythicFixtureState', 'MythicBehaviorChecks', 'MythicGuardChecks'])
    run(javac, ['-J-Xmx256m', '--release', '17', '-encoding', 'UTF-8', '-proc:none', '-cp', os.pathsep.join(cp), '-d', fixture, *sources], 'compile-fixture')
    runtime = os.pathsep.join([str(fixture), *cp])
    run(java, ['-Xmx192m', '-cp', runtime, 'verification.PrepareMythicFixture', work, fixture], 'prepare-fixture')
    run(java, ['-Xmx256m', '-Xverify:all', '-cp', runtime, 'verification.MythicBehaviorChecks'], 'behavior')
    run(java, ['-Xmx128m', '-Xverify:all', '-cp', runtime, 'verification.MythicGuardChecks', work / 'pins'], 'guard')
    (work / 'report.json').write_text(json.dumps({'status': 'passed', 'nativeMixinApplied': True, 'nativeCallbackBodyPreserved': True,
                                                'completeNativeCallbackAndBuilderExecuted': True, 'worldConfigRegistry': 'API doubles',
                                                'gameStarted': False, 'serverStarted': False}, indent=2), 'utf8')


if __name__ == '__main__':
    main()
