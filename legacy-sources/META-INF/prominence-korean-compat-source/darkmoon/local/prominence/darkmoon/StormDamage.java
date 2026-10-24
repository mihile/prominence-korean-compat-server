package local.prominence.darkmoon;

/** Only Arrow Storm's own spawned arrows receive a fixed pre-mitigation amount. */
public interface StormDamage {
    void prominence$setStormDamage(double damage);
    void prominence$setCloudDamage(double damage);
}
