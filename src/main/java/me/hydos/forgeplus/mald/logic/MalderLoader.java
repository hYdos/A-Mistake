package me.hydos.forgeplus.mald.logic;

import me.hydos.forgeplus.mald.logic.read.Malder;
import me.hydos.forgeplus.mald.logic.read.MalderReaderVisitor;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Not to be confused with Mald Loader.
 */
public class MalderLoader {

    public final Map<String, Malder> classToMalderMap = new HashMap<>();

    public void loadMalder(Path classFile) throws IOException {
        byte[] bytes = Files.readAllBytes(classFile);

        MalderReaderVisitor visitor = new MalderReaderVisitor();
        ClassReader reader = new ClassReader(bytes);
        reader.accept(visitor, 0);

        visitor.getTarget().bytecode = bytes;
        classToMalderMap.put(visitor.getTarget().targetClass, visitor.getTarget());
    }

    public MalderPatcher getPatcher(String className) {
        Malder malder = classToMalderMap.get(className);
        if (malder == null) {
            return null;
        }
        return new MalderPatcher(malder);
    }
}
