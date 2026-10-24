package dev.mihile.tridentback;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.Assume;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import static org.junit.Assert.*;

/** Class-file inspection only; no Minecraft server or world is launched. */
public class InstalledTargetsTest {
    private final Path server = Path.of("../../.fabric/remappedJars/minecraft-1.20.1-0.19.3/server-intermediary.jar");
    private ClassNode read(ZipFile zip, String name) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(zip.getInputStream(zip.getEntry(name + ".class"))).accept(node, 0); return node;
    }

    @Test public void allMappedHooksExistInInstalledMinecraft() throws Exception {
        Assume.assumeTrue(Files.exists(server));
        var mappings = RescueConfig.GSON.fromJson(Files.readString(Path.of("build/classes/java/main/give-my-trident-back-refmap.json")), JsonObject.class)
                .getAsJsonObject("mappings");
        try (ZipFile zip = new ZipFile(server.toFile())) {
            for (var mixin : mappings.entrySet()) for (var entry : mixin.getValue().getAsJsonObject().entrySet()) {
                String selector = entry.getValue().getAsString();
                int separator = selector.indexOf(';'), args = selector.indexOf('(');
                if (args < 0) {
                    assertEquals("Shadow owner field", 1, read(zip, "net/minecraft/class_1676").fields.stream()
                            .filter(f -> f.name.equals("field_22478") && f.desc.equals("Ljava/util/UUID;")).count());
                    continue;
                }
                boolean qualified = selector.startsWith("L") && separator < args;
                var node = read(zip, qualified ? selector.substring(1, separator) : "net/minecraft/class_1665");
                String name = selector.substring(qualified ? separator + 1 : 0, args), descriptor = selector.substring(args);
                assertEquals(selector, 1, node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(descriptor)).count());
            }
        }
    }

    @Test public void optionalSimplyIntegrationHasExactRealWeaponFlagAndPickupOverride() throws Exception {
        Path simply = Path.of("../../mods/simplyswords-fabric-1.70.2-1.20.1.jar");
        Assume.assumeTrue(Files.exists(simply));
        try (ZipFile zip = new ZipFile(simply.toFile())) {
            var node = read(zip, "net/sweenus/simplyswords/entity/ThrownSwordEntity");
            assertEquals(1, node.fields.stream().filter(f -> f.name.equals("nonReturning") && f.desc.equals("Z")).count());
            assertEquals(1, node.methods.stream().filter(m -> m.name.equals("method_34713") && m.desc.equals("(Lnet/minecraft/class_1657;)Z")).count());
            assertEquals(1, node.methods.stream().filter(m -> m.name.equals("method_7445") && m.desc.equals("()Lnet/minecraft/class_1799;")).count());
            assertEquals(WeaponFamily.SIMPLY_SWORDS, WeaponFamily.fromClassName(node.name.replace('/', '.')));
        }
    }

    @Test public void soulsReturningWeaponsUseNativeArrowPickup() throws Exception {
        Path souls = Path.of("../../mods/soulslike-weaponry-1.3.1-1.20.1-fabric.jar");
        Assume.assumeTrue(Files.exists(souls));
        try (ZipFile zip = new ZipFile(souls.toFile())) {
            var node = read(zip, "net/soulsweaponry/entity/projectile/ReturningProjectile");
            assertEquals(1, node.methods.stream().filter(m -> m.name.equals("method_34713") && m.desc.equals("(Lnet/minecraft/class_1657;)Z")).count());
            assertEquals(WeaponFamily.SOULS_WEAPONRY, WeaponFamily.fromClassName(node.name.replace('/', '.')));
            for (String name : new String[]{"LeviathanAxeEntity", "MjolnirProjectile"})
                assertEquals(node.name, read(zip, "net/soulsweaponry/entity/projectile/" + name).superName);
            assertNotEquals(node.name, read(zip, "net/soulsweaponry/entity/projectile/DraupnirSpearEntity").superName);
        }
    }

    @Test public void recallUsesOneNativePickupAndNoManualItemCreationOrDrop() throws Exception {
        ClassNode controller = new ClassNode();
        new ClassReader(Files.readAllBytes(Path.of("build/classes/java/main/dev/mihile/tridentback/RescueController.class"))).accept(controller, 0);
        int pickup = 0, teleport = 0;
        for (var method : controller.methods) for (var instruction : method.instructions) {
            if (instruction instanceof TypeInsnNode type) assertNotEquals("net/minecraft/world/item/ItemStack", type.desc);
            if (!(instruction instanceof MethodInsnNode call)) continue;
            assertNotEquals("net/minecraft/world/entity/player/Inventory", call.owner);
            assertFalse("No manual item spawn", call.name.equals("spawnAtLocation"));
            if (call.name.equals("playerTouch")) pickup++;
            if (call.owner.equals("net/fabricmc/fabric/api/dimension/v1/FabricDimensions") && call.name.equals("teleport")) teleport++;
        }
        assertEquals(1, pickup); assertEquals(1, teleport);
    }
}
