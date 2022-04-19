package me.hydos.forgeplus.util;

import me.hydos.forgeplus.ForgePatcher;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.Tiny2Writer;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.minecraftforge.installertools.ConsoleTool;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MappingUtils {
    public static Path getSrgMappings() throws IOException {
        Path srgToIntermediaryMappings = ForgePatcher.WORK_DIR.resolve("srgToIntermediary.tiny");
        if (!Files.exists(srgToIntermediaryMappings)) {
            // Create SRG mappings
            // TODO: pull from http://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/1.18.2/mcp_config-1.18.2.zip
            // TODO: and find official file. Its in one of the libraries folders.
            // TODO: for now assume its already extracted in the work directory
            Path mcpConfig = ForgePatcher.WORK_DIR.resolve("joined.tsrg");
            Path official = Paths.get("C:/Users/hayde/AppData/Roaming/.minecraft/libraries/net/minecraft/client/1.18.2-20220404.173914/client-1.18.2-20220404.173914-mappings.txt");
            Path officialToSrg = createSrgMappings(mcpConfig, official);
            Path intermediaries = ForgePatcher.WORK_DIR.resolve("intermediary.tiny");


            MemoryMappingTree intermediaryMappings = new MemoryMappingTree();
            MappingReader.read(intermediaries, intermediaryMappings);

            MemoryMappingTree srgMappings = new MemoryMappingTree();
            MappingReader.read(officialToSrg, srgMappings);

            MemoryMappingTree srgToIntermediary = new MemoryMappingTree();
            srgToIntermediary.visitNamespaces("srg", List.of("intermediary"));

            for (MappingTree.ClassMapping srgMapping : ((MappingTree) srgMappings).getClasses()) {
                MappingTree.ClassMapping intermediaryMapping = intermediaryMappings.getClass(srgMapping.getSrcName());
                int leftNamespaceId = srgMappings.getNamespaceId("left");
                int srgNamespaceId = srgMappings.getNamespaceId("right");
                int intermediaryNamespaceId = intermediaryMappings.getNamespaceId("intermediary");

                String srgClassName = srgMapping.getDstName(srgNamespaceId);
                srgToIntermediary.visitClass(srgClassName);

                MappingTree.ClassMapping mergedClass = ((MappingTree) srgToIntermediary).getClass(srgClassName);
                mergedClass.setDstName(intermediaryMapping != null ? intermediaryMapping.getDstName(intermediaryNamespaceId) : srgMapping.getSrcName(), srgToIntermediary.getNamespaceId("intermediary"));

                for (MappingTree.FieldMapping field : srgMapping.getFields()) {
                    MappingTree.FieldMapping fieldIntermediaryMapping = null;
                    if (intermediaryMapping != null) {
                        fieldIntermediaryMapping = intermediaryMapping.getField(field.getSrcName(), field.getSrcDesc(), leftNamespaceId);
                    }

                    String srgFieldName = field.getDstName(srgNamespaceId);
                    String srgFieldDesc = field.getDstDesc(srgNamespaceId);
                    String intermediaryFieldName = intermediaryMapping != null && fieldIntermediaryMapping != null ? fieldIntermediaryMapping.getDstName(intermediaryNamespaceId) : field.getSrcName();
                    srgToIntermediary.visitField(
                            srgFieldName,
                            ""
                    );
                    MappingTree.FieldMapping fieldMapping = srgToIntermediary.getField(mergedClass.getSrcName(), srgFieldName, srgFieldDesc);
                    fieldMapping.setDstName(intermediaryFieldName, srgNamespaceId);
                }

                for (MappingTree.MethodMapping method : srgMapping.getMethods()) {
                    MappingTree.MethodMapping methodIntermediaryMapping = null;
                    if (intermediaryMapping != null) {
                        methodIntermediaryMapping = intermediaryMapping.getMethod(method.getSrcName(), method.getSrcDesc(), leftNamespaceId);
                    }

                    String srgFieldName = method.getDstName(srgNamespaceId);
                    String srgFieldDesc = method.getDstDesc(srgNamespaceId);
                    String intermediaryName = intermediaryMapping != null && methodIntermediaryMapping != null ? methodIntermediaryMapping.getDstName(intermediaryNamespaceId) : method.getSrcName();
                    srgToIntermediary.visitMethod(srgFieldName, srgFieldDesc);

                    MappingTree.MethodMapping methodMapping = srgToIntermediary.getMethod(mergedClass.getSrcName(), srgFieldName, srgFieldDesc);
                    methodMapping.setDstName(intermediaryName, intermediaryNamespaceId);
                }
            }

            try (Writer mappingWriter = Files.newBufferedWriter(srgToIntermediaryMappings)) {
                srgToIntermediary.accept(new Tiny2Writer(mappingWriter, false));
            }
        }

        return srgToIntermediaryMappings;
    }

    private static Path createSrgMappings(Path mcpConfig, Path official) throws IOException {
        Path mappings = ForgePatcher.WORK_DIR.resolve("officialToSrg.tsrg");
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
