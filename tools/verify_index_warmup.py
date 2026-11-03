"""Execute production startup scheduling/task with isolated API and executor doubles."""
from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
def main():
 p=argparse.ArgumentParser()
 for n in ('jar','classpath','java-home','mods','output'):p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=True)
 sources={
 'net/minecraft/class_2960.java':'package net.minecraft;public class class_2960 {public final String name;public class_2960(String n){name=n;}}',
 'net/minecraft/class_5321.java':'package net.minecraft;public class class_5321 {private final class_2960 value;public class_5321(class_2960 v){value=v;}public class_2960 method_29177(){return value;}}',
 'net/minecraft/class_1937.java':'package net.minecraft;public class class_1937 {}',
 'net/minecraft/class_3218.java':'package net.minecraft;public class class_3218 extends class_1937 {public final me.cortex.voxy.commonImpl.WorldIdentifier id=new me.cortex.voxy.commonImpl.WorldIdentifier();public final class_2960 dimension=new class_2960("overworld");public class_5321 method_27983(){return new class_5321(dimension);}}',
 'net/minecraft/server/MinecraftServer.java':'package net.minecraft.server;public class MinecraftServer{public boolean onThread=true;public net.minecraft.class_3218 level=new net.minecraft.class_3218();public boolean method_18854(){return onThread;}public net.minecraft.class_3218 method_30002(){return level;}}',
 'me/cortex/voxy/commonImpl/WorldIdentifier.java':'package me.cortex.voxy.commonImpl;public class WorldIdentifier {public static boolean absent;public static WorldIdentifier of(net.minecraft.class_1937 world){return absent?null:((net.minecraft.class_3218)world).id;}}',
 'me/cortex/voxy/common/world/WorldEngine.java':'package me.cortex.voxy.common.world;public class WorldEngine {}',
 'com/dripps/voxyserver/server/ServerLodEngine.java':'''package com.dripps.voxyserver.server;
import me.cortex.voxy.commonImpl.WorldIdentifier;import me.cortex.voxy.common.world.WorldEngine;
public class ServerLodEngine implements local.prominence.indexwarmup.IndexPreparationAccess {
 public int gets,prepares;public boolean noWorld,failOpen,failPrepare;public WorldIdentifier seenId;public net.minecraft.class_2960 seenDim;public final WorldEngine world=new WorldEngine();
 public WorldEngine getOrCreate(WorldIdentifier id,net.minecraft.class_2960 dim){gets++;seenId=id;seenDim=dim;if(failOpen)throw new IllegalStateException("fixture open failure");return noWorld?null:world;}
 public boolean prominence$prepareIndex(WorldIdentifier id,WorldEngine w,Runnable beat){if(w!=world||id!=seenId)throw new AssertionError();prepares++;beat.run();if(failPrepare)throw new IllegalStateException("fixture scan failure");return true;}
}''',
 'IndexTests.java':r'''import java.util.*;import java.util.concurrent.*;import java.lang.reflect.*;
import local.prominence.indexwarmup.*;import local.prominence.indexwarmup.mixin.LodStartupIndexMixin;
import com.dripps.voxyserver.server.ServerLodEngine;import net.minecraft.server.MinecraftServer;
public class IndexTests {
 static int checks;static void check(boolean x,String why){if(!x)throw new AssertionError(why);checks++;}
 static class QueueExecutor extends AbstractExecutorService{boolean stopped;final List<Runnable> pending=new ArrayList<>();public void execute(Runnable r){if(stopped)throw new RejectedExecutionException();pending.add(r);}public void shutdown(){stopped=true;}public List<Runnable> shutdownNow(){stopped=true;var out=List.copyOf(pending);pending.clear();return out;}public boolean isShutdown(){return stopped;}public boolean isTerminated(){return stopped&&pending.isEmpty();}public boolean awaitTermination(long t,TimeUnit u){return isTerminated();}}
 static Object field(Object o,String name)throws Exception{var f=LodStartupIndexMixin.class.getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String name,Object value)throws Exception{var f=LodStartupIndexMixin.class.getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
 static LodStartupIndexMixin service(ServerLodEngine e,QueueExecutor q)throws Exception{var s=new LodStartupIndexMixin(){};set(s,"engine",e);set(s,"streamExecutor",q);return s;}
 public static void main(String[] args)throws Exception{
  var engine=new ServerLodEngine();var q=new QueueExecutor();var service=service(engine,q);var server=new MinecraftServer();var original=server.level;
  server.onThread=false;service.prominence$warmStartup(server);check(q.pending.isEmpty(),"no world metadata read on worker/wrong thread");
  server.onThread=true;service.prominence$warmStartup(server);check(q.pending.size()==1,"one startup job queued");check(engine.gets==0&&engine.prepares==0,"no disk/scan work inline on server thread");
  service.prominence$warmStartup(server);check(q.pending.size()==1,"duplicate startup coalesced");
  server.level=new net.minecraft.class_3218();q.pending.remove(0).run();check(engine.gets==1&&engine.prepares==1,"native preparation requested once");check(engine.seenId==original.id&&engine.seenDim==original.dimension,"immutable default-world identity captured on server thread");check((long)field(service,"lastStreamHeartbeat")>0,"existing worker heartbeat maintained");
  for(var f:IndexWarmupTask.class.getDeclaredFields())check(!f.getType().getName().equals("net.minecraft.server.MinecraftServer")&&!f.getType().getName().equals("net.minecraft.class_3218"),"task retains no live world/server field");
  var otherEngine=new ServerLodEngine();var otherQ=new QueueExecutor();var other=service(otherEngine,otherQ);other.prominence$warmStartup(new MinecraftServer());check(otherQ.pending.size()==1,"new service/server independent");
  otherQ.shutdownNow();check(otherEngine.gets==0,"native shutdown drops unstarted queued job");
  var rejectedQ=new QueueExecutor();rejectedQ.shutdown();var rejected=service(new ServerLodEngine(),rejectedQ);rejected.prominence$warmStartup(new MinecraftServer());check(rejectedQ.pending.isEmpty(),"shutdown executor not resurrected");
  var absentQ=new QueueExecutor();var absent=service(new ServerLodEngine(),absentQ);var missing=new MinecraftServer();missing.level=null;absent.prominence$warmStartup(missing);check(absentQ.pending.isEmpty(),"missing default world leaves normal fallback");
  me.cortex.voxy.commonImpl.WorldIdentifier.absent=true;absent.prominence$warmStartup(new MinecraftServer());check(absentQ.pending.isEmpty(),"missing world identifier leaves normal fallback");me.cortex.voxy.commonImpl.WorldIdentifier.absent=false;
  var cancelledEngine=new ServerLodEngine();var task=new IndexWarmupTask(cancelledEngine,original.id,original.dimension,()->{});Thread.currentThread().interrupt();try{task.run();check(cancelledEngine.gets==0,"already-interrupted worker does no work");check(Thread.currentThread().isInterrupted(),"interrupt not cleared");}finally{Thread.interrupted();}
  var emptyEngine=new ServerLodEngine();emptyEngine.noWorld=true;new IndexWarmupTask(emptyEngine,original.id,original.dimension,()->{}).run();check(emptyEngine.prepares==0,"no world avoids scan");
  var failedEngine=new ServerLodEngine();failedEngine.failOpen=true;new IndexWarmupTask(failedEngine,original.id,original.dimension,()->{}).run();check(failedEngine.prepares==0,"open failure swallowed without scan");
  failedEngine.failOpen=false;failedEngine.failPrepare=true;new IndexWarmupTask(failedEngine,original.id,original.dimension,()->{}).run();check(failedEngine.prepares==1,"scan failure nonfatal and not automatically retried");
  System.out.println("PASS: "+checks+" production startup scheduler/task checks; immutable capture, asynchronous work, heartbeat, service identity, shutdown, interruption and fallback; API doubles, no Minecraft/world launched");
 }
}'''}
 files=[]
 for n,s in sources.items():
  f=out/'src'/n;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(s,encoding='utf-8');files.append(str(f))
 classes=out/'classes';classes.mkdir(exist_ok=True)
 cp=';'.join([str(classes),str(a.jar.resolve()),*[str(p.resolve()) for p in a.mods.glob('*.jar')],*json.loads(a.classpath.read_text())])
 opts=['--release','17','-proc:none','-encoding','UTF-8','-cp',cp,'-d',str(classes),*files]
 f=out/'compile.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in opts))
 subprocess.run([str(a.java_home/'bin/javac.exe'),'@'+str(f)],check=True)
 f=out/'run.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in ['-Xmx256m','-Xverify:all','-cp',cp,'IndexTests']))
 run=subprocess.run([str(a.java_home/'bin/java.exe'),'@'+str(f)],cwd=out,capture_output=True,text=True)
 (out/'scheduler.log').write_text(run.stdout+run.stderr);assert run.returncode==0,run.stdout+run.stderr;print(run.stdout)
if __name__=='__main__':main()
