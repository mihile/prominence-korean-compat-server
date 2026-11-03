package verification;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/** Executes actual native and transformed filter bytecode without loading a game. */
public final class SlotNativeChecks {
    private static volatile long sink;
    private static int checks;

    private static MethodHandle filter(byte[] bytes) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, node.name, null, "java/lang/Object", null);
        for (MethodNode method : node.methods) {
            if (method.name.equals("filterGroupInfo") || method.name.contains("reuseSlotNamePattern")) method.accept(writer);
        }
        writer.visitEnd();
        class FilterLoader extends ClassLoader {
            FilterLoader() { super(SlotNativeChecks.class.getClassLoader()); }
            Class<?> define(byte[] code) { return defineClass(node.name.replace('/', '.'), code, 0, code.length); }
        }
        Class<?> type = new FilterLoader().define(writer.toByteArray());
        return MethodHandles.publicLookup().findStatic(type, "filterGroupInfo", MethodType.methodType(String.class, String.class));
    }

    private static String call(MethodHandle method, String value) throws Throwable {
        return (String) method.invokeExact(value);
    }

    private static void compare(MethodHandle original, MethodHandle patched, String value) throws Throwable {
        String expected = call(original, value), actual = call(patched, value);
        if (!expected.equals(actual)) throw new AssertionError("slot changed: " + value);
        if (expected == value && actual != value) throw new AssertionError("no-match identity changed");
        checks++;
    }

    private static long allocation(MethodHandle method, String[] values, int count) throws Throwable {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().getId(), start = bean.getThreadAllocatedBytes(thread), total = 0;
        for (int i = 0; i < count; i++) total += call(method, values[i % values.length]).length();
        long bytes = bean.getThreadAllocatedBytes(thread) - start;
        sink = total;
        return bytes;
    }

    public static void run(byte[] originalBytes, byte[] patchedBytes) throws Throwable {
        MethodHandle original = filter(originalBytes), patched = filter(patchedBytes);
        String[] cases = {"", "ring", "hand", "necklace", "charm", "trinket_group_", "trinket_group_head-ring",
                "prefix-trinket_group_head-ring-extra", "trinket_group_a-x-trinket_group_b-y", "trinket_group_a",
                "trinket_group_-", "trinket_group_a-\ntrinket_group_b-c", "trinket_group_a\nb-c",
                "trinket_group_a\rb-c", "trinket_group_a\u0085b-c", "trinket_group_a\u2028b-c",
                "trinket_group_a\u2029b-c", "trinket_group_\uD83D\uDE00-궁수", "trinket_group_a-\uD800",
                "trinket_group_a-\uDC00", "x\0trinket_group_a-ring", "trinket_group_a--", "trinket_group_a-\nlast-"};
        for (String value : cases) compare(original, patched, value);
        String alphabet = "abc_trinketgroup-/$\\\n\r\u0085\u2028\u2029\0궁수\uD83D\uDE00";
        Random random = new Random(20261103);
        for (int i = 0; i < 100000; i++) {
            StringBuilder value = new StringBuilder();
            for (int j = random.nextInt(100); j > 0; j--) value.append(alphabet.charAt(random.nextInt(alphabet.length())));
            if ((i & 1) == 0) value.insert(random.nextInt(value.length() + 1), "trinket_group_");
            if ((i & 3) == 0) value.append("-ring");
            compare(original, patched, value.toString());
        }
        for (MethodHandle method : List.of(original, patched)) {
            try { call(method, null); throw new AssertionError("null accepted"); }
            catch (NullPointerException expected) { checks++; }
        }
        var executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Integer>> jobs = new ArrayList<>();
            for (int t = 0; t < 4; t++) jobs.add(() -> {
                try {
                    for (int i = 0; i < 5000; i++) {
                        String value = cases[i % cases.length];
                        if (!call(original, value).equals(call(patched, value))) throw new AssertionError("concurrent mismatch");
                    }
                } catch (Throwable error) { throw new RuntimeException(error); }
                return 5000;
            });
            for (var result : executor.invokeAll(jobs)) checks += result.get();
        } finally { executor.shutdownNow(); }
        String[] representative = {"ring", "hand", "necklace", "charm", "belt", "trinket_group_head-hat"};
        allocation(original, representative, 20000);
        allocation(patched, representative, 20000);
        long before = allocation(original, representative, 100000), after = allocation(patched, representative, 100000);
        if (after >= before / 2) throw new AssertionError("fixture allocation did not decrease: " + before + " -> " + after);
        System.out.println("PASS: " + checks + " native/transformed equality, identity, null and concurrent checks");
        System.out.println("FIXTURE allocation for 100000 calls: " + before + " -> " + after + " bytes; not production timing");
    }
}
