package me.hydos.focu.mald.logic;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MalderMarker extends ClassVisitor {

    protected MalderMarker(MalderWriterVisitor visitor) {
        super(Opcodes.ASM9, visitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return super.visitMethod(access, name, descriptor, "MALD", exceptions);
    }
}
