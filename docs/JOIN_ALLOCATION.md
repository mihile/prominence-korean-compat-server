# LOD packet sizing and unused Invoke tick work

Build2026110307 follows a second120-second server JFR and a post-trace HPROF analyzed in Eclipse MAT. The previous scratch/cache patch was observed in native stack traces and heap ownership, not inferred merely from its installed filename.

## LOD packet capacity

The remaining streaming byte-array allocations include `UnpooledHeapByteBuf.capacity` beneath native `LODBulkPayload.write`. `PreSerializedLodPayload.fromBulk` starts at256bytes, expands while encoding and then makes its independently owned output byte array. The patch redirects only that initial `Unpooled.buffer(int,int)` call to choose a capacity based on the actual native wire layout: UTF-8 dimension name, VarInt section count,16bytes of section key/hash, VarInt lookup arrays, light bytes and the bit-packed short-index longs. Native limits, writer, output copying and finally-release remain unchanged. Invalid/mismatched/oversized estimator inputs fall back to the original initial capacity, preserving native validation. There is no retained pooled buffer and no packet-format change.

The installed port.1/port.2 packet classes are hash-pinned independently of the optional Invoke path. Six actual transformed `fromBulk` executions per variant produce byte-identical output.3,385 native protocol checks cover LUT bit-width and VarInt boundaries, up to16 sections, roundtrip decoding and oversized payload failures. A ThreadMXBean fixture including estimation overhead reports36,877,600 baseline allocated bytes versus21,338,144 pre-sized bytes per100 test packets (about42% less in that fixture, not a whole-server allocation/TPS prediction).

## Unused Invoke cooldown calculation

Invoke1.0.3.1's `lambda$onInitialize$5` calculates `SpellHelper.getCooldownDuration(player, magic_missile) * 20` into float local5. Native ASM verification proves one FSTORE5 and no FLOAD5: the result is never consumed by either missile or target processing. The calculation nevertheless traverses haste providers, equipment, spell containers, enchantments and Accessories/Trinkets adapters each tick. In the capture it appears in74 of3,321 server-thread samples and about310MB of weighted allocation samples. This is a sampled cost, not retained memory.

A redirect replaces this one unused calculation with zero. All other instructions in the original tick callback and all actual spell-cast cooldown paths remain native. Native InvokeMod/SpellHelper hashes are required. Real Mixin transformation verifies that exactly this callback invocation is redirected and the unused-local condition holds. This is a structural proof and offline transform test, not a live gameplay test.

Existing221 compatibility classes and updater bytes remain byte-identical. The release passes the cumulative132-check updater suite,225-class/1,171-method ASM verification and prior guard tests. The normal signed updater installs the release before the next normal server start; running server files and operational settings are not replaced.

The retest still records an initial approximately2.1-second login backlog. Native entity ticks, first synchronization and chunk work remain in the trace. These changes reduce proven avoidable work; they do not claim that every login stall has been eliminated.
