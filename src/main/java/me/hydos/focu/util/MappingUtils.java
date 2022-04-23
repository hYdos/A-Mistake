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

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class MappingUtils {

    public static void fillInSignatures(Path srcJar, String dstNamespace, MemoryMappingTree mappings) throws IOException {
        try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(srcJar); Stream<Path> classes = Files.walk(fs.get().getPath("/")).filter(path -> path.toString().endsWith(".class"))) {
            classes.forEach(path -> {
                try {
                    ClassReader reader = new ClassReader(Files.readAllBytes(path));
                    ClassVisitor visitor = new SignatureFillingVisitor(dstNamespace, mappings);
                    reader.accept(visitor, 0);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read class while filling in signatures", e);
                }
            });
        }
    }

    /**
     * Generates official -> srg -> intermediary
     *
     * @return A path to a tinyv2 file containing mappings
     * @throws IOException File IO may fail.
     */
    public static Path getFullMappings(Path srgMinecraftJar) throws IOException {
        Path fullMappingsPath = ForgePatcher.WORK_DIR.resolve("complete.tiny");

        if (!Files.exists(fullMappingsPath)) {
            // TODO: pull from http://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/1.18.2/mcp_config-1.18.2.zip
            // TODO: and pull from https://maven.fabricmc.net/net/fabricmc/intermediary/1.18.2/
            // TODO: for now assume its already extracted in the work directory
            Path mcpConfig = ForgePatcher.WORK_DIR.resolve("joined.tsrg");
            Path official = Paths.get("C:/Users/hayde/AppData/Roaming/.minecraft/libraries/net/minecraft/client/1.18.2-20220404.173914/client-1.18.2-20220404.173914-mappings.txt");
            Path officialToSrg = createForgeMappings(mcpConfig, official);
            Path intermediaries = ForgePatcher.WORK_DIR.resolve("mappings.tiny");

            MemoryMappingTree fullMappings = new MemoryMappingTree();
            MappingReader.read(intermediaries, fullMappings);
            fullMappings.setSrcNamespace("left");
            MappingReader.read(officialToSrg, fullMappings);
            fullMappings.setSrcNamespace("official");
            fullMappings.setDstNamespaces(List.of("intermediary", "srg"));
            fillInSignatures(srgMinecraftJar, "srg", fullMappings);

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

    public static String remapFieldDescToSrc(MappingTree mappings, String methodDesc, int fromNamespace) {
        int pointer = 0;
        StringBuilder result = new StringBuilder();
        while (pointer < methodDesc.length()) {
            if (methodDesc.charAt(pointer) == 'L') {
                String className = methodDesc.substring(1, methodDesc.indexOf(";"));
                MappingTree.ClassMapping classMapping = mappings.getClass(className, fromNamespace);
                if(classMapping != null) {
                    className = classMapping.getSrcName();
                }
                result.append("L").append(className).append(";");
            } else {
                result.append(methodDesc.charAt(pointer));

            }
            pointer++;
        }
        return result.toString();
    }

    private static class SignatureFillingVisitor extends ClassVisitor {

        private final MemoryMappingTree mappings;
        private final int dstNamespace;
        private String className;

        protected SignatureFillingVisitor(String dstNamespace, MemoryMappingTree mappings) {
            super(ForgePatcher.ASM_VERSION);
            this.mappings = mappings;
            this.dstNamespace = this.mappings.getNamespaceId(dstNamespace);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.className = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            MappingTree.FieldMapping field = this.mappings.getField(this.className, name, descriptor, this.dstNamespace);
            if(field == null) {
                throw new RuntimeException("BROKEN JAR!!! REINSTALL FORGE");
            }
            if (field.getSrcDesc() == null) {
                field.setSrcDesc(remapFieldDescToSrc(this.mappings, descriptor, this.dstNamespace));
            }
            return super.visitField(access, name, descriptor, signature, value);
        }
    }
}
