/* SPDX-License-Identifier: MIT */
package local.prominence.serverlog;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.AbstractFilter;
public final class ServerLogNoise implements PreLaunchEntrypoint {
    public static final class NoiseFilter extends AbstractFilter {
        private final Map<String,Long> shown=new HashMap<>();
        private final LongSupplier clock;
        public NoiseFilter(){this(System::nanoTime);}
        public NoiseFilter(LongSupplier clock){this.clock=clock;}
        public synchronized boolean suppress(String logger,Level level,String text,Throwable thrown) {
            if(thrown!=null||text==null||text.length()>4096)return false;
            String key=null;
            if(level==Level.WARN&&"Lithium Class Analysis".equals(logger)
                    &&text.startsWith("Lithium Class Analysis Error: Class ")
                    &&text.contains("NoClassDefFoundError: net/minecraft/class_")
                    &&text.endsWith("Lithium handles this error by assuming the class cannot be included in some optimizations."))key=text;
            if(level==Level.INFO&&"Voxy".equals(logger)) {
                if(text.matches("\\[me\\.cx\\.vy\\.cl\\.VoxyInstance\\]: Creating new world engine: -?[0-9]+@[0-9]+"))key="voxy/open";
                else if(text.matches("\\[me\\.cx\\.vy\\.cl\\.VoxyInstance\\]: Shutting down idle world: -?[0-9]+"))key="voxy/idle";
            }
            if(key==null)return false;
            long now=clock.getAsLong();Long last=shown.get(key);
            if(last!=null&&now-last>=0&&now-last<300_000_000_000L)return true;
            if(shown.size()<256||last!=null)shown.put(key,now);
            return false;
        }
        @Override public Result filter(LogEvent e) {
            return suppress(e.getLoggerName(),e.getLevel(),e.getMessage().getFormattedMessage(),e.getThrown())?Result.DENY:Result.NEUTRAL;
        }
    }
    @Override public void onPreLaunch() {
        if(Boolean.getBoolean("prominence.serverLogNoise.disabled"))return;
        try {
            LoggerContext context=(LoggerContext)LogManager.getContext(false);
            context.getConfiguration().getRootLogger().addFilter(new NoiseFilter());
            context.updateLoggers();
            LogManager.getLogger("Prominence compatibility").info("Server log noise control: enabled (known repeated messages only; first and every 5 minutes retained)");
        } catch(RuntimeException e) {LogManager.getLogger("Prominence compatibility").warn("Server log noise control skipped",e);}
    }
}
