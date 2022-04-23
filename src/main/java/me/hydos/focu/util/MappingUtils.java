package me.hydos.focu.util;

import me.hydos.focu.ForgePatcher;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.Tiny2Writer;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.minecraftforge.installertools.ConsoleTool;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class MappingUtils {

    public static void fillInFieldSignatures(MemoryMappingTree mappings, Path jar, int namespaceId) throws IOException {
        try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(jar); Stream<Path> classes = Files.walk(fs.get().getPath("/"))) {
            classes.filter(path -> path.getFileName().toString().endsWith(".class")).forEach(path -> {
                try {
                    ClassReader reader = new ClassReader(Files.readAllBytes(path));
                    ClassVisitor visitor = new FieldSignatureFillingVisitor(mappings, namespaceId);
                    reader.accept(visitor, 0);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static class FieldSignatureFillingVisitor extends ClassVisitor {

        private final MemoryMappingTree mappings;
        private final int namespaceId;
        private String className;

        protected FieldSignatureFillingVisitor(MemoryMappingTree mappings, int namespaceId) {
            super(Opcodes.ASM9);
            this.mappings = mappings;
            this.namespaceId = namespaceId;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.className = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            MappingTree.FieldMapping field;
            if(namespaceId == -1) {
                // Src Target Namespace
                field = this.mappings.getField(this.className, name, descriptor);
            } else {
                // Dst Target Namespace
                field = this.mappings.getField(this.className, name, descriptor, this.namespaceId);
            }
            System.out.println(field);
            return super.visitField(access, name, descriptor, signature, value);
        }
    }

    /**
     * Generates official -> srg -> intermediary
     * @return A path to a tinyv2 file containing mappings
     * @throws IOException File IO may fail.
     */
    public static Path getFullMappings() throws IOException {
        Path fullMappingsPath = ForgePatcher.WORK_DIR.resolve("complete.tiny");

        if (!Files.exists(fullMappingsPath)) {
            // TODO: pull from http://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/1.18.2/mcp_config-1.18.2.zip
            // TODO: and find official file. Its in one of the libraries folders.
            // TODO: for now assume its already extracted in the work directory
            Path mcpConfig = ForgePatcher.WORK_DIR.resolve("joined.tsrg");
            Path official = Paths.get("C:/Users/hayde/AppData/Roaming/.minecraft/libraries/net/minecraft/client/1.18.2-20220404.173914/client-1.18.2-20220404.173914-mappings.txt");
            Path officialToSrg = createForgeMappings(mcpConfig, official);
            Path intermediaries = ForgePatcher.WORK_DIR.resolve("intermediary.tiny");

            MemoryMappingTree fullMappings = new MemoryMappingTree();
            MappingReader.read(officialToSrg, fullMappings);
            MappingReader.read(intermediaries, fullMappings);

            try (Writer mappingWriter = Files.newBufferedWriter(fullMappingsPath)) {
                fullMappings.accept(new Tiny2Writer(mappingWriter, false));
            }
        }
        return fullMappingsPath;
    }

    /**
     * Forge's mappings are a combination of Mojmap Classes and Searge Fields and Methods.
     */
    private static Path createForgeMappings(Path mcpConfig, Path official) throws IOException {
        Path mappings = ForgePatcher.WORK_DIR.resolve("forge.tsrg");
        ConsoleTool.main(new String[]{
                "--task",
                "MERGE_MAPPING",
                "--left",
                mcpConfig.toAbsolutePath().toString(),
                "--right",
                official.toAbsolutePath().toString(),
                "--classes",
                "--reverse-right",
                "--output",
                mappings.toAbsolutePath().toString()
        });
        return mappings;
    }
}
