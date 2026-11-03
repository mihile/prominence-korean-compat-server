# Mythic Upgrades global config refresh

Server build2026110312 coalesces Mythic Upgrades4.2.0+mc1.20.1's world-independent equipment refresh once per global server tick. In the saved120-second recording, `ItemEffectsListBuilder` construction and its equipment-buff insertion paths accounted for about0.56GB of weighted server-thread allocations. This is temporary allocation traffic, not retained heap.

The actual installed native `ServerWorldMixin.updateByConfigs` creates12 effect lists, calls53 equipment-effect setters and5 percentage setters whenever a server world ticks. It reads global configuration and writes global registered items; the callback does not read its world instance. Repeating it for other worlds in the same server tick constructs the same global data again. Native setters inspected alongside it assign effect/percentage fields.

## Behavior preserved

The original complete callback remains in place. A successful invocation marks that server/tick, and later world invocations in the same tick return immediately. The next server tick runs it again, so live configuration is still refreshed every tick. A setting changed between worlds within the same tick becomes visible at the next global tick, rather than being rebuilt separately in each world. This is a tick boundary, not a promise of a fixed wall-clock delay under lag.

Completion is recorded only at the original normal return. Exceptions are not swallowed, and a failed partial refresh does not prevent another invocation from retrying. Null-server/off-thread calls retain native execution without entering the cache. Server replacement and tick-counter wrap are handled; the cache uses a weak server reference, keeps no world/player/item references and synchronizes its tiny identity/tick state.

No world ticks, entity ticks, spawns, equipment applications, stats or durations are skipped or nerfed. This is not a spawn limiter. Other compatibility modules, settings, world data and JVM flags are unchanged. `prominence.mythic.configRefresh=false` opts out; the release does not edit user JVM arguments.

## Guard and integration

The plugin requires Minecraft1.20.1, Mythic Upgrades4.2.0+mc1.20.1 and exact reviewed class hashes in `prominence-mythic-refresh-pins.json`. Its low-priority marker Mixin allows a post-apply wrapper to locate the native merged callback by its source annotation, independent of generated handler IDs. An unknown/missing/world-dependent callback, wrong signature or repeated wrapping is left unchanged. The regular native callback body is retained rather than reimplemented.

The upstream repository's [1.20.1 branch at a3be2308](https://github.com/TriQue06/mythic-upgrades-1.20.x/tree/a3be2308eb349dccffb31a97b5d2cdeaa2e5903f) reports4.0.3, while its main branch reports Minecraft1.20.4/4.0.2. They are architectural references, not proof of the installed4.2.0 build. Actual installed metadata/bytecode and exact fingerprints are the authority for this patch. No upstream mod classes are included in the release.

## Validation

Real Sponge Mixin applies the native Mythic world Mixin and the compatibility post-apply wrapper to the actual Minecraft ServerWorld class.163 transformed methods pass ASM verification.162 other methods retain identical instructions/control flow; all native refresh-body instructions,12 builders and53 effect-setter calls are preserved behind the new gate. Unexpected shape and double-wrap checks keep the callback unchanged.

The complete original and transformed native refresh bodies and the original effect builder/metadata classes are executed with small server/config/registry/item doubles.54 checks compare58 global item outputs, each of16 configuration inputs and restoration, same-tick/next-tick changes, native failure/retry, off-thread fallback, server replacement, counter wrap and weak-reference collection.16 production guard checks cover matching/changed/missing versions/classes, linkage failure and opt-out.

A35-world×100-tick fixture performs3,500→100 full refreshes; measured fixture allocation is30,898,984→1,551,752bytes. These are fixture results, not a production RAM/TPS prediction. The production module does not add a new configuration snapshot cache or change equipment formulas. Actual allocation and latency after installation remain to measure.

The cumulative server updater checks132 cases,245 classes/1,253 methods and11 Hourglass cases. All241 previous classes and previous non-metadata resources are preserved byte-for-byte. Raw profiles and fixture outputs stay local.

```text
python tools/integrate_mythic_refresh.py --base LATEST_PUBLIC_SERVER_JAR --output STAGING/integrated.jar --classpath CLASSPATH_JSON --java-home JDK_HOME
python tools/verify_mythic_refresh.py --jar STAGING/integrated.jar --classpath CLASSPATH_JSON --java-home JDK_HOME --mods SERVER_MODS --output STAGING/verification
python tools/build_cumulative_metadata.py --base STAGING/integrated.jar --output STAGING/release --build NEW_BUILD
python tools/verify.py --build-dir STAGING/release --classpath CLASSPATH_JSON --java-home JDK_HOME
```

Publication uses the existing signed server release/feed and installation occurs immediately before the next normal server start. No running JAR replacement or automatic server restart is performed. The module is server-only; this does not publish or install another client build.
