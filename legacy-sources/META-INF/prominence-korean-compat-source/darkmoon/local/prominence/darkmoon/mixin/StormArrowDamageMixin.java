package local.prominence.darkmoon.mixin;

import local.prominence.darkmoon.StormDamage;
import net.minecraft.class_1665;
import net.minecraft.class_2487;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = class_1665.class, remap = false, priority = 1100)
public abstract class StormArrowDamageMixin implements StormDamage {
    @Unique private static final String prominence$NBT_KEY = "ProminenceDarkmoonStormDamage";
    @Unique private static final String prominence$CLOUD_NBT_KEY = "ProminenceDarkmoonCloudDamage";
    @Unique private double prominence$stormDamage = Double.NaN;
    @Unique private double prominence$cloudDamage = Double.NaN;
    @Shadow private double field_7571;

    @Unique private static double prominence$bounded(double damage) {
        return Double.isFinite(damage)
            ? Math.max(0.0D, Math.min(Integer.MAX_VALUE, damage)) : Double.NaN;
    }

    @Override public void prominence$setStormDamage(double damage) {
        prominence$stormDamage = prominence$bounded(damage);
    }

    @Override public void prominence$setCloudDamage(double damage) {
        prominence$cloudDamage = prominence$bounded(damage);
        field_7571 = prominence$cloudDamage;
    }

    // The cloud is a carrier, not a damaging arrow. Attributes must apply to
    // each actual arrow once, rather than to the carrier and then its arrows.
    @Inject(method = "method_7438", at = @At("HEAD"), cancellable = true, require = 1)
    private void prominence$keepCloudBase(double incoming, CallbackInfo ci) {
        if (Double.isFinite(prominence$cloudDamage)) {
            field_7571 = prominence$cloudDamage;
            ci.cancel();
        }
    }

    // Replace the initial velocity-derived amount, before crit and hit hooks.
    // DamageSource, protection, resistance, piercing and hit callbacks stay native.
    // Run before Ranged Weapon API's priority-1000 critical-damage modifier.
    @ModifyVariable(method = "method_7454", at = @At(value = "STORE", ordinal = 0), ordinal = 0, require = 1)
    private int prominence$fixedStormImpact(int incoming) {
        return Double.isFinite(prominence$stormDamage)
                && Double.isFinite(field_7571)
            ? (int) Math.ceil(prominence$bounded(field_7571)) : incoming;
    }

    @Inject(method = "method_5652", at = @At("TAIL"), require = 1)
    private void prominence$writeStormDamage(class_2487 nbt, CallbackInfo ci) {
        if (Double.isFinite(prominence$stormDamage)) {
            nbt.method_10549(prominence$NBT_KEY, prominence$stormDamage);
        }
        if (Double.isFinite(prominence$cloudDamage)) {
            nbt.method_10549(prominence$CLOUD_NBT_KEY, prominence$cloudDamage);
        }
    }

    @Inject(method = "method_5749", at = @At("TAIL"), require = 1)
    private void prominence$readStormDamage(class_2487 nbt, CallbackInfo ci) {
        prominence$stormDamage = Double.NaN;
        prominence$cloudDamage = Double.NaN;
        if (nbt.method_10573(prominence$NBT_KEY, 6)) {
            prominence$setStormDamage(nbt.method_10574(prominence$NBT_KEY));
        }
        if (nbt.method_10573(prominence$CLOUD_NBT_KEY, 6)) {
            prominence$setCloudDamage(nbt.method_10574(prominence$CLOUD_NBT_KEY));
        }
    }
}
