package verification;

import java.util.Arrays;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.util.Configuration;
import local.prominence.sparkbackground.SparkBackgroundGuard;

/** Actual constructor and unchanged lifecycle bytecode, with tiny platform/config doubles. */
public final class SparkNativeChecks {
    private static int checks;
    private static void check(boolean result) { if (!result) throw new AssertionError("spark check " + checks); checks++; }
    private static Class<?> constructorClass(byte[] bytes) {
        ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node, 0);
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, node.name, null, "java/lang/Object", null);
        for (var field : node.fields) field.accept(writer);
        for (var method : node.methods) {
            if (method.name.equals("<init>") || method.name.contains("disableContinuousJavaSampling")) method.accept(writer);
        }
        writer.visitEnd();
        class ConstructorLoader extends ClassLoader {
            ConstructorLoader() { super(SparkNativeChecks.class.getClassLoader()); }
            Class<?> define() { byte[] code = writer.toByteArray(); return defineClass(node.name.replace('/', '.'), code, 0, code.length); }
        }
        return new ConstructorLoader().define();
    }
    private static byte[] isolatedMethod(ClassNode node, String name) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, node.name, null, "java/lang/Object", null);
        for (MethodNode method : node.methods) if (method.name.equals(name)) method.accept(writer);
        writer.visitEnd(); return writer.toByteArray();
    }
    public static void run(byte[] original, byte[] patched) throws Exception {
        Class<?> before = constructorClass(original), after = constructorClass(patched);
        for (var type : PlatformInfo.Type.values()) for (boolean configured : new boolean[]{true, false}) {
            var platform = new SparkPlatform(() -> () -> type);
            var oldConfig = new Configuration(configured); var newConfig = new Configuration(configured);
            Object oldObject = before.getConstructor(SparkPlatform.class, Configuration.class).newInstance(platform, oldConfig);
            Object newObject = after.getConstructor(SparkPlatform.class, Configuration.class).newInstance(platform, newConfig);
            var oldEnabled = before.getDeclaredField("enabled"); oldEnabled.setAccessible(true);
            var newEnabled = after.getDeclaredField("enabled"); newEnabled.setAccessible(true);
            check(oldEnabled.getBoolean(oldObject) == (type != PlatformInfo.Type.CLIENT && configured));
            check(!newEnabled.getBoolean(newObject));
            check(oldConfig.calls == newConfig.calls);
            check(newConfig.configured == configured); // No config mutation.
        }
        ClassNode raw = new ClassNode(), changed = new ClassNode();
        new ClassReader(original).accept(raw, 0); new ClassReader(patched).accept(changed, 0);
        for (String name : new String[]{"initialise", "restartBackgroundSampler", "startSampler"}) {
            check(Arrays.equals(isolatedMethod(raw, name), isolatedMethod(changed, name)));
        }
        String background = "88a8d60a1ff46901ed866997a8c933f44b8fa73bf0ed7d44b94b42e35f233fb2";
        String access = "dc4550511b64147067e3ce26d6e1c70f1d60ed46720f7446e34fb4da310c41c1";
        check(SparkBackgroundGuard.matches("Windows 11", "1.10.53", background, access, false));
        check(SparkBackgroundGuard.matches("WINDOWS 10", "1.10.53", background, access, false));
        for (String os : new String[]{"Linux", "Mac OS X", "Darwin", ""})
            check(!SparkBackgroundGuard.matches(os, "1.10.53", background, access, false));
        check(!SparkBackgroundGuard.matches("Windows 11", "1.10.54", background, access, false));
        check(!SparkBackgroundGuard.matches("Windows 11", "1.10.53", "changed", access, false));
        check(!SparkBackgroundGuard.matches("Windows 11", "1.10.53", background, "changed", false));
        check(!SparkBackgroundGuard.matches("Windows 11", "1.10.53", background, access, true));
        System.out.println("PASS: " + checks + " native spark constructor/config/lifecycle and OS/version/hash/opt-out guard checks");
    }
}
