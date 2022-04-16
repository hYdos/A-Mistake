package me.hydos.forgeplus.mald.logic;

import me.hydos.forgeplus.mald.logic.read.InjectTarget;
import me.hydos.forgeplus.mald.logic.read.Malder;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public class MalderWriterVisitor extends ClassVisitor {

    private final ClassNode malderClass;
    private final Malder malder;

    public MalderWriterVisitor(ClassVisitor parent, ClassNode malderClass, Malder malder, InjectTarget target) {
        super(Opcodes.ASM9, parent);
        this.malderClass = malderClass;
        this.malder = malder;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (this.malder.targetMethods.contains(name)) {
            for (MethodNode method : malderClass.methods) {
                if (!method.name.equals("<init>")) {
                    if (method.invisibleAnnotations.get(0).values.get(1).equals(name)) {
                        method.localVariables.remove(0);
                        return new MethodWriterVisitor(parent, method);
                    }
                }
            }
        }
        return parent;
    }

    private static class MethodWriterVisitor extends MethodVisitor {

        public final MethodNode method;

        protected MethodWriterVisitor(MethodVisitor methodVisitor, MethodNode method) {
            super(Opcodes.ASM9, methodVisitor);
            this.method = method;
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN) {
                for (AbstractInsnNode instruction : this.method.instructions) {
                    if (instruction instanceof LabelNode label) {
                        visitLabel(label.getLabel());
                    } else if (instruction instanceof LineNumberNode lineNumber) {
                        visitLineNumber(lineNumber.line, lineNumber.start.getLabel());
                    } else if (instruction instanceof FieldInsnNode fieldInsn) {
                        visitFieldInsn(fieldInsn.getOpcode(), fieldInsn.owner, fieldInsn.name, fieldInsn.desc);
                    } else if (instruction instanceof LdcInsnNode ldcInsnNode) {
                        visitLdcInsn(ldcInsnNode.cst);
                    } else if (instruction instanceof MethodInsnNode methodInsn) {
                        visitMethodInsn(methodInsn.getOpcode(), methodInsn.owner, methodInsn.name, methodInsn.desc, methodInsn.getOpcode() == Opcodes.INVOKEINTERFACE);
                    } else if (instruction instanceof VarInsnNode varInsn) {
                        visitVarInsn(varInsn.getOpcode(), varInsn.var);
                    } else if (instruction instanceof IntInsnNode intInsn) {
                        visitIntInsn(intInsn.getOpcode(), intInsn.operand);
                    } else if (instruction instanceof TypeInsnNode typeInsn) {
                        visitTypeInsn(typeInsn.getOpcode(), typeInsn.desc);
                    } else if (instruction instanceof InsnNode insn) {
                        if (insn.getOpcode() != Opcodes.RETURN) {
                            visitInsn(insn.getOpcode());
                        }
                    } else {
                        throw new RuntimeException("Unknown Instruction Type: " + instruction.getClass().getName());
                    }
                }
            }

            super.visitInsn(opcode);
        }
    }
}
