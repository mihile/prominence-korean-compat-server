package local.prominence.voxyservercalm;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Prevents VoxyServer from releasing active WorldEngine references on every idle tick
 * (which causes 10-second RocksDB shutdown/reopen cycles while players are online)
 * and bounds synchronous chunk-section deep-copies per server tick during CHUNK_LOAD bursts.
 */
public final class VoxyServerCalmState {
    public static final int MAX_SYNC_LOAD_INGESTS_PER_TICK = 8;

    private static long activeTick = Long.MIN_VALUE;
    private static int usedInTick = 0;
    private static final AtomicLong DEFERRED_LOAD_INGESTS = new AtomicLong();

    private VoxyServerCalmState() {}

    public static synchronized boolean tryAcquireLoadIngest(long tick) {
        if (tick != activeTick) {
            activeTick = tick;
            usedInTick = 0;
        }
        if (usedInTick < MAX_SYNC_LOAD_INGESTS_PER_TICK) {
            usedInTick++;
            return true;
        }
        DEFERRED_LOAD_INGESTS.incrementAndGet();
        return false;
    }

    public static synchronized void reset() {
        activeTick = Long.MIN_VALUE;
        usedInTick = 0;
        DEFERRED_LOAD_INGESTS.set(0L);
    }

    public static long deferredCount() {
        return DEFERRED_LOAD_INGESTS.get();
    }
}
