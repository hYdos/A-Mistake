package me.hydos.focu.mald.logic;

import me.hydos.focu.mald.logic.read.InjectTarget;
import me.hydos.focu.mald.logic.read.Malder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/**
 * This class is a class which is designed to visit a method when needed. This is useful because we want to visit a method and for it to translate into another method, Hence injecting bytecode.
 */
public record MalderPatcher(Malder malder) {

    public byte[] patch(byte[] classBytes, InjectTarget target) {
        // Setup for Reading and Writing actual class
        ClassReader classReader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        // Setup for Reading the Malder Class
        ClassNode clazz = new ClassNode();
        ClassReader malderReader = new ClassReader(this.malder.bytecode);
        malderReader.accept(clazz, 0);

        // Setup visitor which visits both readers.
        classReader.accept(new MalderWriterVisitor(writer, clazz, this.malder, target), 0);

        return writer.toByteArray();
    }
}
