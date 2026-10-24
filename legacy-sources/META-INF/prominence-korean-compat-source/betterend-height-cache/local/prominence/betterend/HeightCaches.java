package local.prominence.betterend;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Only cache storage changes; height calculation and world access stay in BetterEnd. */
public final class HeightCaches {
    private HeightCaches() {}
    public static <V> Map<Integer, V> concurrentCopy(Map<Integer, V> original) {
        return new ConcurrentHashMap<>(original);
    }
}
