package me.hydos.forgeplus.mald.logic.read;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MalderReaderVisitor extends ClassVisitor {

    private final TargetClassAnnotationVisitor annotationVisitor;
    private final MethodAnnotationVisitor methodAnnotationVisitor;

    public MalderReaderVisitor() {
        super(Opcodes.ASM9);
        this.annotationVisitor = new TargetClassAnnotationVisitor();
        this.methodAnnotationVisitor = new MethodAnnotationVisitor(this.annotationVisitor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return annotationVisitor;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return methodAnnotationVisitor;
    }

    public Malder getTarget() {
        return this.annotationVisitor.malder;
    }

    private static class MethodAnnotationVisitor extends MethodVisitor {

        private final AnnotationVisitor annotationVisitor;

        protected MethodAnnotationVisitor(AnnotationVisitor visitor) {
            super(Opcodes.ASM9);
            this.annotationVisitor = visitor;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return this.annotationVisitor;
        }
    }

    private static class TargetClassAnnotationVisitor extends AnnotationVisitor {

        public Malder malder = new Malder();

        protected TargetClassAnnotationVisitor() {
            super(Opcodes.ASM9);
        }

        // FIXME: assumptions bad. Please fix them soon
        @Override
        public void visit(String name, Object value) {
            String val = (String) value;
            if(val.startsWith("m_")) {
                this.malder.targetMethods.add(val);
            } else {
                this.malder.targetClass = val;
            }

            super.visit(name, value);
        }
    }
}
