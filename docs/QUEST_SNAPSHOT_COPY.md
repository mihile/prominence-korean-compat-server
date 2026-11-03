# Prominent quest snapshot allocation

Server build2026110313 adds a guarded, server-only change to Prominent4.1.1 on Minecraft1.20.1. The pack version remains4.1.3. Installed bytecode, rather than an unverified newer source branch, defines this patch.

`QuestProgress.open` first deep-copies the complete persistent player data through `PersistentPlayerData.get`. It then deep-copies the complete `prominent_quests` subtree, and finally deep-copies the selected quest. The middle copy duplicates an already private snapshot. The selected quest remains independently detached until `save`.

The patch removes only that middle `NbtCompound.copy` invocation. The complete player snapshot, selected quest copy, legacy migration, missing/wrong-type handling, timers, waypoint progress, reset/delete behavior and all native save code remain. The quest-list subtree is private within the already detached player snapshot and is mutated by native `save`, which then attaches it to that same snapshot. No shared player-data reference, result cache, tick skipping or changed quest priority is introduced. Existing stale-snapshot behavior across separately opened progress objects is preserved, rather than silently changing save semantics.

The plugin checks Minecraft1.20.1, Prominent4.1.1, and exact raw fingerprints of `QuestProgress` and `PersistentPlayerData`. After the marker Mixin applies, it compares the entire native `open` instruction/control-flow shape with the pinned original before removing the first of its two copy calls. Other transformations or unknown shapes retain native behavior. The complete initial snapshot relies on the reviewed native `PersistentPlayerData.get` contract; mods that independently replace that contract are outside the reviewed combination. `-Dprominence.quest.snapshotCopy=false` opts out without changing any operating configuration.

## Validation

Real Sponge Mixin transforms the installed native QuestProgress class.47 methods pass ASM verification;46 other native methods retain identical instructions/control flow. An independent instruction comparison checks that exactly the intermediate copy is removed and branch destinations are preserved. Missing/signature/changed/double-application shapes are rejected.

Both the original and transformed complete native classes execute with real Minecraft NBT, original PersistentPlayerData and original FauxCustomEntityData CustomDataHelper. Only entity shells implement the native holder interfaces as doubles.421 behavior assertions in each process and identical state digests cover unsaved isolation, old persistent snapshot isolation, saves, stale interleaved snapshots, unrelated data, active/completed flags, counters, objectives, sequences, timers, longs, waypoints, resets/deletes, legacy migration and malformed roots, plus400 deterministic mixed operations.

A separate80-quest-tree×1,000-open allocation fixture measures87,888,000→43,712,000bytes. This is fixture allocation, not a prediction of retained RAM or the full server workload.12 production guard checks cover version/hash/missing/linkage/opt-out behavior. Existing245 classes and non-metadata entries are preserved byte-for-byte. Cumulative verification covers132 updater cases,248 classes/1,269 methods and11 Hourglass cases. A small filesystem fixture also verifies the real signed build12→13 artifacts, the original backup, three preparations with only one download, and no startup-attempt marker. No game/server/world was started.

The new capture showed at least307MB of sampled weighted allocation with a visible QuestProgress.open caller and72MB through PersistentPlayerData.get (overlapping categories, not added). Recursive NBT stacks can hide callers past the analyzer's30-frame display limit; these are allocation estimates, not retained heap. The retained dump was about1.54GiB, with Voxy caches, recipes and class loading among its largest owners. The patch does not claim to remove these retained owners or fix the entire9,002ms accumulated post-login backlog. Actual latency/allocation after installation remain to measure.

```text
python tools/integrate_quest_copy.py --base LATEST_PUBLIC_SERVER_JAR --output STAGING/integrated.jar --classpath CLASSPATH_JSON --java-home JDK_HOME
python tools/verify_quest_copy.py --jar STAGING/integrated.jar --classpath CLASSPATH_JSON --java-home JDK_HOME --mods SERVER_MODS --output STAGING/verification
python tools/build_cumulative_metadata.py --base STAGING/integrated.jar --output STAGING/release --build NEW_BUILD
python tools/verify.py --build-dir STAGING/release --classpath CLASSPATH_JSON --java-home JDK_HOME
```

The verifier accepts paired `--upgrade-base OLD_PUBLIC_JAR --upgrade-manifest SIGNED_NEW_MANIFEST` arguments to repeat the real-artifact filesystem check in a fresh output directory.

The signed server release/feed publishes the cumulative artifact. It is eligible for the next normal startup update; the running JAR is not replaced and the server is not stopped/restarted. Client artifacts, settings, worlds, JVM flags and other mods are unchanged. Raw captures, local classpaths and fixture outputs are not included in public artifacts.
