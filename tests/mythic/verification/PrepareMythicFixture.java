package verification;
import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public final class PrepareMythicFixture {
    public static void main(String[] args)throws Exception {
        Path work=Path.of(args[0]),classes=Path.of(args[1]);
        for(String mode:new String[]{"before","fixed"}){
            ClassNode source=new ClassNode();new ClassReader(Files.readAllBytes(work.resolve(mode+"-world.class"))).accept(source,0);
            MethodNode callback=source.methods.stream().filter(m->m.name.endsWith("$updateByConfigs")).findFirst().orElseThrow();
            String name="verification/"+(mode.equals("before")?"NativeRefresh":"FixedRefresh");
            ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
            writer.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,name,null,"net/minecraft/class_3218",null);
            var ctor=writer.visitMethod(Opcodes.ACC_PUBLIC,"<init>","(Lnet/minecraft/server/MinecraftServer;)V",null,null);ctor.visitCode();
            ctor.visitVarInsn(Opcodes.ALOAD,0);ctor.visitVarInsn(Opcodes.ALOAD,1);ctor.visitMethodInsn(Opcodes.INVOKESPECIAL,"net/minecraft/class_3218","<init>","(Lnet/minecraft/server/MinecraftServer;)V",false);ctor.visitInsn(Opcodes.RETURN);ctor.visitMaxs(2,2);ctor.visitEnd();
            callback.name="run";callback.access=Opcodes.ACC_PUBLIC;callback.visibleAnnotations=null;callback.invisibleAnnotations=null;callback.accept(writer);writer.visitEnd();
            Path file=classes.resolve(name+".class");Files.createDirectories(file.getParent());Files.write(file,writer.toByteArray());
        }
        System.out.println("Prepared complete original/fixed native refresh callbacks with small server/config/registry doubles");
    }
}
