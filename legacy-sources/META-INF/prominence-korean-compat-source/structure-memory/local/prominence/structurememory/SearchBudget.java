package local.prominence.structurememory;

/** Tick-thread-only scheduling; no search radius, target, result or generation rule changes. */
public final class SearchBudget {
    private static long started;
    private static int samples;
    public static void beginTick() { started = System.nanoTime(); samples = 0; }
    public static boolean trySample() {
        Runtime vm = Runtime.getRuntime();
        long used = vm.totalMemory() - vm.freeMemory();
        if (used >= vm.maxMemory() * 9L / 10L) return false;
        if (samples >= 8 || started == 0 || System.nanoTime() - started >= 2_000_000L) return false;
        ++samples;
        return true;
    }
}
