package verification;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import local.prominence.mythicrefresh.RefreshGate;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class MythicBehaviorChecks {
    static int checks;
    static final CallbackInfo callback=new CallbackInfo("fixture",false);
    static void check(boolean value){if(!value)throw new AssertionError("check "+checks);checks++;}
    static void call(Object world)throws Exception {
        try{world.getClass().getMethod("run",CallbackInfo.class).invoke(world,callback);}
        catch(InvocationTargetException e){if(e.getCause() instanceof RuntimeException r)throw r;throw e;}
    }
    static Object world(String kind,MinecraftServer server)throws Exception{return Class.forName("verification."+kind+"Refresh").getConstructor(MinecraftServer.class).newInstance(server);}
    static WeakReference<MinecraftServer> weakServer(){MinecraftServer server=new MinecraftServer();RefreshGate.completed(server);return new WeakReference<>(server);}
    public static void main(String[] args)throws Exception {
        var server=new MinecraftServer();var baseline=world("Native",server);call(baseline);
        var expected=MythicFixtureState.snapshot();var warm=new MinecraftServer();call(world("Fixed",warm));check(expected.equals(MythicFixtureState.snapshot()));
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().getId();
        MythicFixtureState.resetCounts();long start=bean.getThreadAllocatedBytes(thread);
        for(int tick=0;tick<100;tick++){server.ticks=tick;for(int dimension=0;dimension<35;dimension++)call(baseline);}
        long nativeBytes=bean.getThreadAllocatedBytes(thread)-start;check(MythicFixtureState.effectWrites==100*35*53);check(MythicFixtureState.percentWrites==100*35*5);
        var fixedServer=new MinecraftServer();var fixed=world("Fixed",fixedServer);MythicFixtureState.resetCounts();start=bean.getThreadAllocatedBytes(thread);
        for(int tick=0;tick<100;tick++){fixedServer.ticks=tick;for(int dimension=0;dimension<35;dimension++)call(fixed);}
        long fixedBytes=bean.getThreadAllocatedBytes(thread)-start;check(MythicFixtureState.effectWrites==100*53);check(MythicFixtureState.percentWrites==100*5);check(nativeBytes>fixedBytes*10);check(expected.equals(MythicFixtureState.snapshot()));
        // Every native configuration input still produces the same complete global item state.
        var keys=new ArrayList<>(MythicFixtureState.config.keySet());check(keys.size()==16);
        for(var key:keys){
            var old=MythicFixtureState.config.get(key);MythicFixtureState.config.put(key,old.doubleValue()+2.25);
            call(baseline);var changed=MythicFixtureState.snapshot();fixedServer.ticks++;call(fixed);check(changed.equals(MythicFixtureState.snapshot()));
            MythicFixtureState.config.put(key,old);call(baseline);fixedServer.ticks++;call(fixed);check(expected.equals(MythicFixtureState.snapshot()));
        }
        // Same-tick changes are deliberately reflected at the next global server tick.
        String key=keys.get(0);var old=MythicFixtureState.config.get(key);MythicFixtureState.config.put(key,9);
        call(fixed);check(expected.equals(MythicFixtureState.snapshot()));fixedServer.ticks++;call(fixed);var next=MythicFixtureState.snapshot();call(baseline);check(next.equals(MythicFixtureState.snapshot()));
        MythicFixtureState.config.put(key,old);fixedServer.ticks++;call(fixed);check(expected.equals(MythicFixtureState.snapshot()));
        // Only successful completion marks a tick. A partial native setter failure can retry.
        fixedServer.ticks++;MythicFixtureState.failNext=true;
        try{call(fixed);throw new AssertionError("setter failure hidden");}catch(IllegalStateException e){check(e.getMessage().equals("fixture setter failure"));}
        check(RefreshGate.shouldRun(fixedServer));call(fixed);check(!RefreshGate.shouldRun(fixedServer));check(expected.equals(MythicFixtureState.snapshot()));
        fixedServer.onThread=false;MythicFixtureState.resetCounts();call(fixed);call(fixed);check(MythicFixtureState.effectWrites==106);fixedServer.onThread=true;
        var replacement=new MinecraftServer();replacement.ticks=fixedServer.ticks;check(RefreshGate.shouldRun(replacement));call(world("Fixed",replacement));check(!RefreshGate.shouldRun(replacement));
        replacement.ticks=Integer.MAX_VALUE;call(world("Fixed",replacement));replacement.ticks=Integer.MIN_VALUE;check(RefreshGate.shouldRun(replacement));call(world("Fixed",replacement));check(!RefreshGate.shouldRun(replacement));
        check(RefreshGate.shouldRun(null));RefreshGate.completed(null);
        var weak=weakServer();for(int i=0;i<20&&weak.get()!=null;i++){System.gc();Thread.sleep(10);}check(weak.get()==null);
        System.out.println("PASS: "+checks+" complete native refresh state/config/failure/thread/server-lifetime checks across "+expected.size()+" item outputs");
        System.out.println("FIXTURE: 35 worlds x 100 ticks, refreshes 3500 -> 100; allocation "+nativeBytes+" -> "+fixedBytes+" bytes, not production RAM/timing");
    }
}
