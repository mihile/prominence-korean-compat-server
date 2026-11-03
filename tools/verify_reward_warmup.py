"""Run production warmup lifecycle and native reward codec with small API doubles."""
from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
def main():
 p=argparse.ArgumentParser()
 for n in ('jar','classpath','java-home','mods','output'):p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=True)
 sources={
 'net/fabricmc/fabric/api/event/Event.java':'''package net.fabricmc.fabric.api.event; public class Event<T>{public final java.util.List<T> callbacks=new java.util.ArrayList<>();public void register(T cb){callbacks.add(cb);}}''',
 'net/fabricmc/fabric/api/event/lifecycle/v1/ServerLifecycleEvents.java':'''package net.fabricmc.fabric.api.event.lifecycle.v1; import net.fabricmc.fabric.api.event.Event; import net.minecraft.server.MinecraftServer; public class ServerLifecycleEvents{public interface ServerStarted{void onServerStarted(MinecraftServer s);}public interface ServerStopped{void onServerStopped(MinecraftServer s);}public static final Event<ServerStarted> SERVER_STARTED=new Event<>();public static final Event<ServerStopped> SERVER_STOPPED=new Event<>();}''',
 'net/fabricmc/fabric/api/event/lifecycle/v1/ServerTickEvents.java':'''package net.fabricmc.fabric.api.event.lifecycle.v1;import net.fabricmc.fabric.api.event.Event;import net.minecraft.server.MinecraftServer;public class ServerTickEvents{public interface EndTick{void onEndTick(MinecraftServer s);}public static final Event<EndTick> END_SERVER_TICK=new Event<>();}''',
 'net/minecraft/class_1863.java':'''package net.minecraft;public class class_1863{public java.util.List<local.prominence.rewards.RewardDefinition> plans=java.util.List.of();public boolean fail;}''',
 'net/minecraft/server/MinecraftServer.java':'''package net.minecraft.server;public class MinecraftServer{public boolean onThread=true;public final net.minecraft.class_1863 recipes=new net.minecraft.class_1863();public boolean method_18854(){return onThread;}public net.minecraft.class_1863 method_3772(){return recipes;}}''',
 'local/prominence/rewards/RewardRegistry.java':'''package local.prominence.rewards;public class RewardRegistry{public static int snapshots;public static java.util.List<RewardDefinition> snapshot(net.minecraft.class_1863 recipes){snapshots++;if(recipes.fail)throw new IllegalStateException("fixture failure");return recipes.plans;}}''',
 'WarmupTests.java':r'''import java.util.*;
import local.prominence.rewards.*;import local.prominence.rewardwarmup.*;
import net.minecraft.server.MinecraftServer;import net.fabricmc.fabric.api.event.lifecycle.v1.*;
public class WarmupTests {
 static int checks;static void check(boolean x,String msg){if(!x)throw new AssertionError(msg);checks++;}
 static void start(MinecraftServer s){for(var c:ServerLifecycleEvents.SERVER_STARTED.callbacks)c.onServerStarted(s);}
 static void tick(MinecraftServer s){for(var c:ServerTickEvents.END_SERVER_TICK.callbacks)c.onEndTick(s);}
 static void stop(MinecraftServer s){for(var c:ServerLifecycleEvents.SERVER_STOPPED.callbacks)c.onServerStopped(s);}
 static RewardDefinition plan(String item){return new RewardDefinition("minecraft:entities/zombie","chance",.1,.4,.15,true,List.of(new RewardDefinition.Output(item,1,1,0,true)),List.of(),List.of(),List.of());}
 public static void main(String[] args)throws Exception{
  new RewardWarmupInit().onInitialize();var s=new MinecraftServer();s.recipes.plans=List.of(plan("minecraft:diamond"));
  tick(s);check(RewardRegistry.snapshots==0,"no preparation before SERVER_STARTED");
  start(s);start(s);check(RewardRegistry.snapshots==0,"lifecycle only queues, safe after all startup callbacks");
  s.onThread=false;tick(s);check(RewardRegistry.snapshots==0,"wrong thread must not traverse recipes");
  s.onThread=true;tick(s);check(RewardRegistry.snapshots==1,"first safe tick prepares once");
  byte[] modern=RewardWire.payload(s.recipes.plans),legacy=RewardWire.legacyPayload(s.recipes.plans);
  check(RewardWire.decode(modern).equals(s.recipes.plans),"production v3 roundtrip");check(RewardWire.decode(legacy).equals(s.recipes.plans),"production v2 roundtrip");
  for(int i=0;i<100;i++)tick(s);start(s);tick(s);check(RewardRegistry.snapshots==1,"no per-tick or duplicate-start rebuild");
  check(RewardWire.payload(s.recipes.plans)==modern,"login reuses prepared v3 bytes");check(RewardWire.legacyPayload(s.recipes.plans)==legacy,"legacy login reuses prepared bytes");
  var other=new MinecraftServer();start(other);tick(s);check(RewardRegistry.snapshots==1,"server identity preserved");tick(other);check(RewardRegistry.snapshots==2,"other server independently prepares");
  var cancelled=new MinecraftServer();start(cancelled);stop(cancelled);tick(cancelled);check(RewardRegistry.snapshots==2,"stopped pending server removed");
  var failed=new MinecraftServer();failed.recipes.fail=true;start(failed);tick(failed);check(RewardRegistry.snapshots==3,"optional failure swallowed once");for(int i=0;i<20;i++)tick(failed);check(RewardRegistry.snapshots==3,"no failure retry storm");
  failed.recipes.fail=false;failed.recipes.plans=List.of(plan("minecraft:emerald"));check(RewardWire.decode(RewardWire.payload(RewardRegistry.snapshot(failed.recipes))).equals(failed.recipes.plans),"normal lazy login fallback recovers");
  s.recipes.plans=List.of(plan("minecraft:gold_ingot"));byte[] reloaded=RewardWire.payload(RewardRegistry.snapshot(s.recipes));check(!Arrays.equals(modern,reloaded),"new reload list invalidates existing native wire cache");check(RewardWire.decode(reloaded).equals(s.recipes.plans),"new metadata correct");
  stop(s);start(s);tick(s);check(RewardRegistry.snapshots==6,"stopped identity can restart without stale lifecycle state");
  System.out.println("PASS: "+checks+" production warmup lifecycle/native codec checks; readiness, safe thread, coalescing, server separation, stop, failure fallback, v2/v3 cache reuse and reload; no Minecraft/world launched");
 }
}'''}
 files=[]
 for name,body in sources.items():
  f=out/'src'/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(body,encoding='utf-8');files.append(str(f))
 classes=out/'classes';classes.mkdir(exist_ok=True)
 cp=';'.join([str(classes),str(a.jar.resolve()),*[str(p.resolve()) for p in a.mods.glob('*.jar')],*json.loads(a.classpath.read_text())])
 options=['--release','17','-proc:none','-encoding','UTF-8','-cp',cp,'-d',str(classes),*files]
 arg=out/'compile.args';arg.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in options))
 subprocess.run([str(a.java_home/'bin/javac.exe'),'@'+str(arg)],check=True)
 arg=out/'run.args';arg.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in ['-Xmx256m','-Xverify:all','-cp',cp,'WarmupTests']))
 run=subprocess.run([str(a.java_home/'bin/java.exe'),'@'+str(arg)],cwd=out,capture_output=True,text=True)
 (out/'warmup.log').write_text(run.stdout+run.stderr);assert run.returncode==0,run.stdout+run.stderr;print(run.stdout)
if __name__=='__main__':main()
