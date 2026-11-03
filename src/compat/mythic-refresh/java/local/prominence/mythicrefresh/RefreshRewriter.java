package local.prominence.mythicrefresh;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Wrap the reviewed native callback after its own Mixin has merged it. */
public final class RefreshRewriter {
    private RefreshRewriter() {}
    private static boolean fromNativeMixin(MethodNode method) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (method.visibleAnnotations != null) annotations.addAll(method.visibleAnnotations);
        if (method.invisibleAnnotations != null) annotations.addAll(method.invisibleAnnotations);
        for (var annotation : annotations) if (annotation.desc.equals("Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;") && annotation.values != null) {
            for (int i = 0; i + 1 < annotation.values.size(); i += 2)
                if (annotation.values.get(i).equals("mixin") && annotation.values.get(i + 1)
                        .equals("net.trique.mythicupgrades.mixin.ServerWorldMixin")) return true;
        }
        return false;
    }
    private static void server(InsnList code) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/class_3218", "method_8503", "()Lnet/minecraft/server/MinecraftServer;", false));
    }
    public static boolean apply(ClassNode node) {
        var matches = node.methods.stream().filter(m -> m.name.endsWith("$updateByConfigs") && fromNativeMixin(m)).toList();
        if (matches.size() != 1) return false;
        MethodNode method = matches.get(0);
        if (!method.desc.equals("(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V")
                || (method.access & Opcodes.ACC_STATIC) != 0) return false;
        List<AbstractInsnNode> returns = new ArrayList<>();
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals("local/prominence/mythicrefresh/RefreshGate")) return false;
            // The native callback must remain entirely world-independent.
            if (instruction instanceof VarInsnNode variable && variable.var == 0) return false;
            if (instruction.getOpcode() == Opcodes.RETURN) returns.add(instruction);
        }
        if (returns.size() != 1) return false;
        LabelNode run = new LabelNode(); InsnList prefix = new InsnList();
        server(prefix);
        prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "local/prominence/mythicrefresh/RefreshGate", "shouldRun", "(Lnet/minecraft/server/MinecraftServer;)Z", false));
        prefix.add(new JumpInsnNode(Opcodes.IFNE, run)); prefix.add(new InsnNode(Opcodes.RETURN));
        prefix.add(run); prefix.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(prefix);
        InsnList tail = new InsnList(); server(tail);
        tail.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "local/prominence/mythicrefresh/RefreshGate", "completed", "(Lnet/minecraft/server/MinecraftServer;)V", false));
        method.instructions.insertBefore(returns.get(0), tail);
        method.maxStack = Math.max(method.maxStack, 2);
        return true;
    }
}
