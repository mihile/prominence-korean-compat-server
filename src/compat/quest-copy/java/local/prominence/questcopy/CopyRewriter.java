package local.prominence.questcopy;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Reuse the private quest-list subtree of an already detached player snapshot. */
public final class CopyRewriter {
    private static final String DESC = "(Lnet/minecraft/class_3222;Lnet/minecraft/class_2960;)Lelocindev/prominent/api/questing/QuestProgress;";
    private CopyRewriter() {}

    private static MethodNode open(ClassNode node) {
        return node.methods.stream().filter(m -> m.name.equals("open") && m.desc.equals(DESC)).findFirst().orElse(null);
    }
    // Reject changes by another transformer, including changed branch destinations.
    private static List<String> shape(MethodNode method) {
        if (method == null || !method.tryCatchBlocks.isEmpty()) return null;
        var labels = new IdentityHashMap<LabelNode, Integer>();
        int pos = 0;
        for (var insn : method.instructions) {
            if (insn instanceof LabelNode label) labels.put(label, pos);
            if (insn.getOpcode() >= 0) pos++;
        }
        var result = new ArrayList<String>();
        result.add("access:" + method.access);
        for (var insn : method.instructions) {
            int op = insn.getOpcode();
            if (op < 0) continue;
            String value = Integer.toString(op);
            if (insn instanceof VarInsnNode n) value += ":" + n.var;
            else if (insn instanceof MethodInsnNode n) value += ":" + n.owner + ":" + n.name + ":" + n.desc + ":" + n.itf;
            else if (insn instanceof TypeInsnNode n) value += ":" + n.desc;
            else if (insn instanceof IntInsnNode n) value += ":" + n.operand;
            else if (insn instanceof LdcInsnNode n) value += ":" + n.cst.getClass().getName() + ":" + n.cst;
            else if (insn instanceof JumpInsnNode n) value += ":" + labels.get(n.label);
            else if (!(insn instanceof InsnNode)) return null;
            result.add(value);
        }
        return result;
    }
    public static boolean apply(ClassNode target, ClassNode reviewed) {
        if (!"elocindev/prominent/api/questing/QuestProgress".equals(target.name) || !target.name.equals(reviewed.name)) return false;
        MethodNode method = open(target), original = open(reviewed);
        List<String> expected = shape(original);
        if (expected == null || !expected.equals(shape(method))) return false;
        var copies = new ArrayList<MethodInsnNode>();
        for (var insn : method.instructions) {
            if (insn instanceof MethodInsnNode n && n.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && n.owner.equals("net/minecraft/class_2487") && n.name.equals("method_10553")
                    && n.desc.equals("()Lnet/minecraft/class_2487;")) copies.add(n);
        }
        if (copies.size() != 2) return false;
        // The complete player copy and selected quest copy remain independent.
        method.instructions.remove(copies.get(0));
        return true;
    }
}
