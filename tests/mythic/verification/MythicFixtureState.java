package verification;
import java.lang.reflect.*;
import java.util.*;

public final class MythicFixtureState {
    public static final Map<String,Number> config=new TreeMap<>();
    public static final List<Item> items=new ArrayList<>();
    public static int effectWrites,percentWrites;
    public static boolean failNext;
    public static Number value(String key){return config.computeIfAbsent(key,k->k.endsWith("amplifier")?Integer.valueOf(1):Float.valueOf(1.5f));}
    public static class Item {
        final String id;Object effects;List<?> amplifiers=List.of();float percent;
        public Item(String id){this.id=id;items.add(this);}
        public void setNewEffects(net.trique.mythicupgrades.util.ItemEffectsList effects){
            if(failNext){failNext=false;throw new IllegalStateException("fixture setter failure");}
            this.effects=effects;effectWrites++;
        }
        public void setNewEffects(net.trique.mythicupgrades.util.ItemEffectsList effects,List<Integer> values){setNewEffects(effects);amplifiers=values;}
        public void setPercent(float value){percent=value;percentWrites++;}
    }
    static String dump(Object value)throws Exception {
        if(value==null)return "null";
        if(value instanceof Map<?,?> map){var entries=new TreeMap<String,String>();for(var e:map.entrySet())entries.put(dump(e.getKey()),dump(e.getValue()));return entries.toString();}
        if(value instanceof List<?> list){var result=new ArrayList<String>();for(var e:list)result.add(dump(e));return result.toString();}
        if(value instanceof Enum<?> e)return e.name();
        if(value.getClass().getName().startsWith("net.trique.mythicupgrades.util.")){
            var fields=new TreeMap<String,String>();for(var field:value.getClass().getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())){field.setAccessible(true);fields.put(field.getName(),dump(field.get(value)));}return fields.toString();
        }
        return value.toString();
    }
    public static Map<String,String> snapshot()throws Exception {
        var values=new TreeMap<String,String>();for(var item:items)values.put(item.id,dump(item.effects)+" / "+dump(item.amplifiers)+" / "+item.percent);return values;
    }
    public static void resetCounts(){effectWrites=percentWrites=0;}
}
