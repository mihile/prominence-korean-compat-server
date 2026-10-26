import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import local.prominence.arrowtick.NeruinaUuid;
import local.prominence.serverlog.ServerLogNoise.NoiseFilter;
import net.minecraft.class_2487;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.layout.PatternLayout;
public final class ArrowNativeTests {
 static int checks;
 static void check(boolean v,String s){if(!v)throw new AssertionError(s);checks++;}
 static final class Capture extends AbstractAppender {
  final List<LogEvent> events=new ArrayList<>();
  Capture(){super("Capture",null,PatternLayout.createDefaultLayout(),false,Property.EMPTY_ARRAY);}
  public void append(LogEvent e){events.add(e.toImmutable());}
 }
 public static void main(String[] args){
  UUID id=UUID.fromString("7e0747ed-6b1b-4ab5-8a3b-747a41426461"),entity=UUID.fromString("300bbbc3-7630-4173-822c-4460aff2773e");
  class_2487 legacy=new class_2487();legacy.method_10582("uuid",id.toString());legacy.method_25927("entityUuid",entity);legacy.method_10582("message","original exception");legacy.method_10544("pos",123456L);
  boolean reproduced=false;try{legacy.method_25926("uuid");}catch(IllegalArgumentException e){reproduced=true;}
  check(reproduced,"native string/getUuid defect not reproduced");
  var fixed=NeruinaUuid.read(legacy);
  check(fixed!=legacy,"read must copy legacy input");check(fixed.method_25926("uuid").equals(id),"legacy ID lost");
  check(legacy.method_10573("uuid",8),"source mutated");check(fixed.method_25926("entityUuid").equals(entity),"entity ID lost");
  check(fixed.method_10558("message").equals("original exception")&&fixed.method_10537("pos")==123456L,"unrelated data changed");
  check(NeruinaUuid.read(fixed)==fixed,"native NBT should be untouched");
  NeruinaUuid.write(legacy);check(legacy.method_10573("uuid",11)&&legacy.method_25926("uuid").equals(id),"new writes remain incompatible");
  var oldEntity=new class_2487();oldEntity.method_10582("entityUuid",entity.toString().toUpperCase(Locale.ROOT));
  check(NeruinaUuid.read(oldEntity).method_25926("entityUuid").equals(entity),"legacy entity ID");
  for(String bad:new String[]{"","oops","1-1-1-1-1"," 7e0747ed-6b1b-4ab5-8a3b-747a41426461"}){
   var n=new class_2487();n.method_10582("uuid",bad);check(NeruinaUuid.read(n)==n,"malformed ID fabricated");NeruinaUuid.write(n);check(n.method_10573("uuid",8),"malformed ID hidden");
  }
  var missing=new class_2487();check(NeruinaUuid.read(missing)==missing,"missing ID fabricated");check(NeruinaUuid.read(null)==null,"null input");NeruinaUuid.write(null);
  AtomicLong clock=new AtomicLong(0);var filter=new NoiseFilter(clock::get);
  String warning="Lithium Class Analysis Error: Class org.betterx.bclib.blocks.BaseBlock cannot be analysed, because getting declared methods crashes with NoClassDefFoundError: net/minecraft/class_793. Lithium handles this error by assuming the class cannot be included in some optimizations.";
  check(!filter.suppress("Lithium Class Analysis",Level.WARN,warning,null),"first warning hidden");
  check(filter.suppress("Lithium Class Analysis",Level.WARN,warning,null),"duplicate warning not filtered");
  check(!filter.suppress("Lithium Class Analysis",Level.WARN,warning.replace("BaseBlock","OtherClass"),null),"new class hidden");
  check(!filter.suppress("different logger",Level.WARN,warning,null),"other logger hidden");
  check(!filter.suppress("Lithium Class Analysis",Level.ERROR,warning,null),"error hidden");
  check(!filter.suppress("Lithium Class Analysis",Level.WARN,warning,new RuntimeException()),"throwable hidden");
  check(!filter.suppress("Lithium Class Analysis",Level.WARN,"Unknown reflection failure",null),"unknown warning hidden");
  clock.set(300_000_000_000L);check(!filter.suppress("Lithium Class Analysis",Level.WARN,warning,null),"periodic diagnostic hidden");
  String open="[me.cx.vy.cl.VoxyInstance]: Creating new world engine: -123@42",close="[me.cx.vy.cl.VoxyInstance]: Shutting down idle world: -123";
  check(!filter.suppress("Voxy",Level.INFO,open,null)&&filter.suppress("Voxy",Level.INFO,open.replace("-123@42","-456@43"),null),"Voxy lifecycle dedupe");
  check(!filter.suppress("Voxy",Level.INFO,close,null)&&filter.suppress("Voxy",Level.INFO,close,null),"idle lifecycle dedupe");
  check(!filter.suppress("Voxy",Level.ERROR,open,null)&&!filter.suppress("Voxy",Level.WARN,open,null),"Voxy errors hidden");
  check(!filter.suppress("Voxy",Level.INFO,"[me.cx.vy.cl.VoxyInstance]: Creating new world engine: failed",null),"changed lifecycle diagnostic hidden");
  for(int i=0;i<300;i++)filter.suppress("Lithium Class Analysis",Level.WARN,warning.replace("BaseBlock","Class"+i),null);
  check(!filter.suppress("Lithium Class Analysis",Level.WARN,warning.replace("BaseBlock","BeyondLimit"),null),"capacity overflow silently hidden");
  var context=new LoggerContext("noise-fixture");var config=new DefaultConfiguration();var root=config.getRootLogger();
  for(String n:List.copyOf(root.getAppenders().keySet()))root.removeAppender(n);
  var capture=new Capture();capture.start();config.addAppender(capture);root.addAppender(capture,Level.ALL,null);root.setLevel(Level.ALL);root.addFilter(new NoiseFilter());
  // Check propagation through a named child logger as well as the root path.
  config.addLogger("Voxy",new LoggerConfig("Voxy",Level.INFO,true));context.start(config);
  var logger=context.getLogger("Voxy");logger.info(open);logger.info(open);logger.error(open);logger.warn("real warning");
  check(capture.events.size()==3,"actual Log4j root filter did not retain 1 INFO + ERROR + WARN: "+capture.events.size());
  var lithium=context.getLogger("Lithium Class Analysis");lithium.warn(warning);lithium.warn(warning);lithium.error("real error");
  check(capture.events.size()==5,"actual Log4j Lithium warning deduplication");context.stop();
  System.out.println("PASS: "+checks+" native NBT and actual Log4j checks; original UUID error reproduced; no game/server/world main");
 }
}
