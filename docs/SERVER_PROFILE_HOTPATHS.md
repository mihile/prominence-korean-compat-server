# Windows continuous sampling and trinket slot parsing

Server build2026110311 adds two narrow modules to the previous cumulative build. The updater, gameplay modules and previous236 classes stay byte-identical. No operational configuration, JVM arguments, world data or other mod JAR is changed.

## Windows spark automatic sampling

The saved two-minute server JFR contained12,007 `ThreadDump` VM operations requested by `spark-java-sampler-*`. Matching their `safepointId` to `jdk.SafepointBegin` gives9.1597152seconds of aggregate safepoint-entry synchronization, with a longest matching event of866.9219ms. The VM-operation bodies themselves took about0.350seconds; omitting entry synchronization misses this cost. Entry synchronization is not a direct measurement of how long every application thread was stopped, nor proof that all login backlog was caused by spark.

The native spark1.10.53 `BackgroundSamplerManager` starts a background sampler at a default10ms interval; `SamplerBuilder` falls back to `JavaSampler` when its async engine is unsupported. On the inspected Windows JVM, the automatic sampler had been running since startup. Official [configuration documentation](https://spark.lucko.me/docs/Configuration) describes the enabled default and interval; [async-profiler requirements](https://spark.lucko.me/docs/misc/Using-async-profiler) list Linux/macOS support.

The module modifies only the `Configuration.getBoolean` expression in the background manager constructor, making its automatic-background `enabled` value false. The native read still occurs and the configuration object/file is not modified. Its existing disabled path handles initialisation and prevents automatic restart after a manual capture ends. Manual sampler commands/builders, TPS/MSPT/statistics, heap features and JFR remain native and available. Continuous pre-command background history is intentionally unavailable while this suppression is active. A manually started Java sampler can still incur sampling overhead.

The guard requires Minecraft1.20.1, spark1.10.53, Windows and exact native `BackgroundSamplerManager`/`AsyncProfilerAccess` SHA-256 fingerprints. Linux, macOS and unknown versions/classes are unchanged. `-Dprominence.spark.backgroundJava=true` opts out and restores native automatic behavior; this release does not add that property or edit user arguments. The existing running sampler is not stopped; the module takes effect at the next normal process start.

## Accessories Trinkets Compat Layer slot names

The actual installed Tclayer3.8.1+beta.14 `WrappingTrinketsUtils.filterGroupInfo` calls `String.replaceAll("(trinket_group_).*-", "")`. Its server-thread allocation samples carry1,072,997,176bytes of estimated cumulative allocation over the saved120seconds. This is temporary allocation traffic, not retained memory or an exact byte count.31 of1,939 server-thread execution samples included this method. The larger compatibility-layer cost also includes fresh inventory wrappers and map materialization; this module does not claim to eliminate those costs.

The new helper shares one immutable `Pattern`. Strings without the required literal prefix return immediately; matching candidates still use the identical native regex and replacement, preserving greedy matches, multiple matches and Unicode/line-terminator semantics. Each match gets a local matcher. No equipment, names, inventory wrappers, entities or query results are cached. Equip/unequip, data reloads and live slot changes keep their native lookup behavior.

Source review used upstream1.20.1-Backport commit [`8f4fb631fc60daf0940e084e35511150bbc6eca7`](https://github.com/wisp-forest/accessories/blob/8f4fb631fc60daf0940e084e35511150bbc6eca7/tclayer/src/main/java/dev/emi/trinkets/compat/WrappingTrinketsUtils.java), whose Tclayer properties also specify3.8.1/beta.14; the actual installed class bytecode remains the authority. A version/class fingerprint guard limits the change to that reviewed native implementation.

## Validation and building

`tools/verify_trinket_slot.py` applies the real Sponge Mixin/MixinExtras engine to both original mod targets, then ASM-verifies all methods. It executes the original and transformed native slot-filter bodies in tiny isolated classes:120,025 comparisons cover seeded random strings, greedy/multiple patterns, Unicode, line terminators, null behavior, unchanged-string identity and concurrent calls. A representative100,000-call fixture measured98,533,264→5,333,120allocated bytes, about94.6% less. This is a fixture allocation result, not a production latency prediction.

The actual original/transformed spark constructor is executed with tiny platform/config doubles.37 checks cover enabled/disabled configuration, server/client/proxy branches, config-read preservation, unchanged native lifecycle method bytecode and OS/version/class-hash/opt-out guards. No Minecraft world/server or native profiler is started. Manual command code is not transformed. Cumulative checks cover132 updater cases,241 classes/1,233 methods and11 existing Hourglass guards.

```text
python tools/integrate_trinket_slot.py --base LATEST_PUBLIC_SERVER_JAR --output STAGING/integrated.jar --classpath classpath.json --java-home JDK_HOME --mods SERVER_MODS
python tools/verify_trinket_slot.py --jar STAGING/integrated.jar --classpath classpath.json --java-home JDK_HOME --mods SERVER_MODS --output STAGING/verification
python tools/build_cumulative_metadata.py --base STAGING/integrated.jar --output STAGING/release --build NEW_BUILD
python tools/verify.py --build-dir STAGING/release --classpath classpath.json --java-home JDK_HOME
```

Publication uses the existing signed server release/feed. Actual login latency, automatic-sampler absence and allocation changes require a later normal-start recording. Raw JFR/HPROF/logs stay local.
