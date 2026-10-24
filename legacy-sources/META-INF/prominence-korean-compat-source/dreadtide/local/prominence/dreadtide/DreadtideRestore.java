package local.prominence.dreadtide;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.extensions.injected.InjectedItemPropertiesExtension;
import net.minecraft.class_1291;
import net.minecraft.class_1293;
import net.minecraft.class_1309;
import net.minecraft.class_1320;
import net.minecraft.class_1322;
import net.minecraft.class_1657;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1814;
import net.minecraft.class_2960;
import net.minecraft.class_4081;
import net.minecraft.class_7923;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.compat.eldritch_end.EldritchEndCompatRegistry;
import net.sweenus.simplyswords.compat.eldritch_end.VoidhungerEffect;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.LegacyWeaponAttributes;
import net.sweenus.simplyswords.item.custom.DreadtideSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

public final class DreadtideRestore {
    public static final class_2960 ITEM_ID = new class_2960("simplyswords", "dreadtide");
    public static final class_2960 HUNGER_ID = new class_2960("simplyswords", "voidhunger");
    private static final class_2960 CORRUPTION_ID = new class_2960("eldritch_end", "corruption");
    private static RegistrySupplier<DreadtideSwordItem> weapon;
    private static RegistrySupplier<class_1291> hunger;
    private static boolean registered;

    /** Called after Config.init, before the existing deferred item/effect registries run. */
    public static void register() {
        if (registered) return;
        if (class_7923.field_41178.method_10250(ITEM_ID)
                || class_7923.field_41174.method_10250(HUNGER_ID)) {
            throw new IllegalStateException("Dreadtide restoration conflicts with an existing registration");
        }
        weapon = ItemsRegistry.ITEM.register("dreadtide", () -> {
            class_1792.class_1793 settings = ((InjectedItemPropertiesExtension)(Object)new class_1792.class_1793())
                    .arch$tab(SimplySwords.SIMPLYSWORDS).method_7894(class_1814.field_8904).method_24359();
            return new DreadtideSwordItem(new AberrationMaterial(), LegacyWeaponAttributes.configure(settings,
                    Config.weaponAttribute.uniqueDamageModifier.dreadtide_damageModifier.get().intValue(),
                    Config.weaponAttribute.uniqueAttackSpeed.dreadtide_attackSpeed.get()));
        });
        hunger = EldritchEndCompatRegistry.EFFECT.register("voidhunger", () -> {
            class_1320 attribute = class_7923.field_41190.method_10223(CORRUPTION_ID);
            if (attribute == null) throw new IllegalStateException("Eldritch End corruption attribute is absent");
            return new VoidhungerEffect(class_4081.field_18272, 1124687).method_5566(attribute,
                    "03360b9d-c99b-4525-826a-ff5da528ebc2", 1.0, class_1322.class_1323.field_6328);
        });
        registered = true;
        SimplySwords.LOGGER.info("[Prominence compatibility] Restored Dreadtide and Voidhunger registrations");
    }

    /** Reconnect the original server tick hook; no extra world/player scan is needed. */
    public static void tick(class_1309 entity) {
        if (!registered || !(entity instanceof class_1657 player) || entity.method_37908().method_8608()) return;
        class_1799 stack = player.method_6047();
        if (!stack.method_31574(weapon.get())) return;
        DreadtideSwordItem.EffectSettings settings = Config.uniqueEffects.dreadtide.get();
        int frequency = Math.max(1, settings.corruptionFrequency);
        if (player.field_6012 % frequency == 0) {
            // Each amplifier level adds exactly one corruption point, using the original effect UUID.
            int cap = Math.max(0, (int) settings.corruptionMax);
            int increment = Math.max(0, (int) settings.corruptionPerTick);
            addStacks(player, hunger.get(), Math.max(1, settings.corruptionDuration), increment, cap);
        }
        if (player.field_6012 % 180 == 0 && AwakeningApi.isAbilityUnlocked(stack)) {
            class_1320 attribute = class_7923.field_41190.method_10223(CORRUPTION_ID);
            if (attribute == null || player.method_5996(attribute) == null) return;
            int cap = Math.max(0, (int) player.method_26825(attribute) / 20);
            if (cap > 0) {
                addStacks(player, EffectRegistry.getReference(EffectRegistry.VOIDCLOAK), 280, 1, cap);
                player.method_37908().method_8396(null, player.method_24515(), SoundRegistry.SPELL_FIRE.get(),
                        player.method_5634(), 0.1f, 1.4f);
            }
        }
    }

    private static void addStacks(class_1309 entity, class_1291 effect, int duration, int increment, int cap) {
        if (cap <= 0 || increment <= 0) return;
        class_1293 previous = entity.method_6112(effect);
        int current = previous == null ? 0 : previous.method_5578() + 1;
        int next = Math.min(cap, current + increment);
        int ticks = previous == null ? duration : Math.max(duration, previous.method_5584());
        entity.method_6092(new class_1293(effect, ticks, next - 1, false, false, true));
    }
    private DreadtideRestore() {}
}
