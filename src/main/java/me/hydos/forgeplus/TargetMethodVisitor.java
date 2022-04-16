package me.hydos.forgeplus;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.function.Function;

public class TargetMethodVisitor extends ClassVisitor {

    private final String methodName;
    private final Function<MethodVisitor, MethodVisitor> methodVisitor;

    public TargetMethodVisitor(String methodName, ClassWriter writer, Function<MethodVisitor, MethodVisitor> methodVisitor) {
        super(Opcodes.ASM9, writer);
        this.methodName = methodName;
        this.methodVisitor = methodVisitor;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (name.equals(methodName)) {
            return methodVisitor.apply(super.visitMethod(access, name, descriptor, signature, exceptions));
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
