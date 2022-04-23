package me.hydos.focu.accesstransformer;

import net.fabricmc.mappingio.format.Tiny2Reader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class AccessTransformer {

    public final Map<ClassTransform, Access> classTransformers;
    public final Map<FieldTransform, Access> fieldTransformers;
    public final Map<MethodTransform, Access> methodTransformers;

    public AccessTransformer() {
        this.classTransformers = new HashMap<>();
        this.fieldTransformers = new HashMap<>();
        this.methodTransformers = new HashMap<>();
    }

    public static AccessTransformer read(Path accessTransformerFile) {
        AccessTransformer accessTransformer = new AccessTransformer();

        try (BufferedReader reader = Files.newBufferedReader(accessTransformerFile)) {
            reader.lines().forEach(line -> {
                if (!line.startsWith("#")) {
                    String[] split = line.split(" ");
                    String rawAccess = split[0];
                    FinalModifier finalModifier = FinalModifier.DO_NOTHING;

                    if (rawAccess.indexOf('-') != -1) {
                        finalModifier = FinalModifier.REMOVE;
                        rawAccess = rawAccess.substring(0, rawAccess.indexOf('-'));
                    }

                    if (rawAccess.indexOf('+') != -1) {
                        finalModifier = FinalModifier.ADD;
                        rawAccess = rawAccess.substring(0, rawAccess.indexOf('+'));
                    }

                    Access access = switch (rawAccess.toLowerCase()) {
                        case "public" -> Access.PUBLIC;
                        case "protected" -> Access.PROTECTED;
                        case "private" -> Access.PRIVATE;
                        case "default" -> Access.PACKAGE_PRIVATE;
                        default -> throw new RuntimeException("Unknown Access Modifier " + rawAccess);
                    };
                    String className = split[1];

                    if (split.length == 2) {
                        // Class Modifier
                        accessTransformer.classTransformers.put(new ClassTransform(className, finalModifier), access);
                    } else {
                        if (split[2].indexOf('(') == -1) {
                            // Field Modifier
                            String fieldName = split[2];
                            accessTransformer.fieldTransformers.put(new FieldTransform(className, fieldName, finalModifier), access);
                        } else {
                            // Method Modifier
                            String methodName = split[2].substring(0, split[2].indexOf('('));
                            String methodSignature = split[2].substring(split[2].indexOf('('));
                            accessTransformer.methodTransformers.put(new MethodTransform(className, methodName, methodSignature, finalModifier), access);
                        }
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return accessTransformer;
    }

    public void remap(Path srgMappingsFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(srgMappingsFile)) {
            MemoryMappingTree srgMappings = new MemoryMappingTree();
            Tiny2Reader.read(reader, srgMappings);

            Map<ClassTransform, Access> classTransformers = new HashMap<>(this.classTransformers);
            Map<FieldTransform, Access> fieldTransformers = new HashMap<>(this.fieldTransformers);
            Map<MethodTransform, Access> methodTransformers = new HashMap<>(this.methodTransformers);

            this.classTransformers.clear();
            this.fieldTransformers.clear();
            this.methodTransformers.clear();
            int intermediary = srgMappings.getNamespaceId("intermediary");

            classTransformers.forEach((classTransform, access) -> {
                MappingTree.ClassMapping classMapping = ((MappingTree) srgMappings).getClass(classTransform.className().replace(".", "/"));
                if (classMapping == null) {
                    System.out.println("No mapping for " + classTransform.className() + ". Not remapping");
                    this.classTransformers.put(classTransform, access);
                } else {
                    this.classTransformers.put(new ClassTransform(classMapping.getDstName(intermediary), classTransform.removeFinal()), access);
                }
            });

            fieldTransformers.forEach((fieldTransform, access) -> {
                MappingTree.ClassMapping classMapping = ((MappingTree) srgMappings).getClass(fieldTransform.className().replace(".", "/"));
                if (classMapping == null) {
                    System.out.println("No mapping for " + fieldTransform.className() + ". Not remapping");
                    this.fieldTransformers.put(fieldTransform, access);
                } else {
                    MappingTree.FieldMapping fieldMapping = getField(classMapping, fieldTransform.field());
                    this.fieldTransformers.put(new FieldTransform(classMapping.getDstName(intermediary), fieldMapping.getDstName(intermediary), fieldTransform.removeFinal()), access);
                }
            });

            methodTransformers.forEach((methodTransform, access) -> {
                MappingTree.ClassMapping classMapping = ((MappingTree) srgMappings).getClass(methodTransform.className().replace(".", "/"));
                if (classMapping == null) {
                    System.out.println("No mapping for " + methodTransform.className() + ". Not remapping");
                    this.methodTransformers.put(methodTransform, access);
                } else {
                    MappingTree.MethodMapping methodMapping = getMethod(classMapping, methodTransform.methodName());
                    this.methodTransformers.put(new MethodTransform(classMapping.getDstName(intermediary), methodMapping.getDstName(intermediary), methodMapping.getDstDesc(intermediary), methodTransform.removeFinal()), access);
                }
            });
        }
    }

    private MappingTree.FieldMapping getField(MappingTree.ClassMapping classMapping, String field) {
        for (MappingTree.FieldMapping fieldMapping : classMapping.getFields()) {
            if (field.equals(fieldMapping.getDstName(1))) {
                return fieldMapping;
            }
        }
        return null;
    }

    private MappingTree.MethodMapping getMethod(MappingTree.ClassMapping classMapping, String methodName) {
        for (MappingTree.MethodMapping methodMapping : classMapping.getMethods()) {
            if (methodName.equals(methodMapping.getDstName(1))) {
                return methodMapping;
            }
        }
        return null;
    }

    private String remapMethodDesc(MappingTree mappings, String methodDesc) {
        int pointer = 1;
        StringBuilder remappedMethodDesc = new StringBuilder("(");
        int intermediary = mappings.getNamespaceId("intermediary");

        while (pointer < methodDesc.length()) {
            String substr = methodDesc.substring(pointer);
            if (substr.startsWith("L")) {
                int classDefEnd = substr.indexOf(';');
                String className = substr.substring(1, classDefEnd);
                System.out.println(className);

                remappedMethodDesc.append("L");
                MappingTree.ClassMapping classMapping = mappings.getClass(className);
                if (classMapping != null) {
                    remappedMethodDesc.append(classMapping.getDstName(intermediary));
                } else {
                    remappedMethodDesc.append(className);
                }
                remappedMethodDesc.append(";");

                pointer += classDefEnd + 1;
            } else {
                remappedMethodDesc.append(substr.charAt(0));
                pointer++;
            }
        }
        return remappedMethodDesc.toString();
    }

    public void write(Path path) {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            this.classTransformers.forEach((classTransform, access) -> {
                try {
                    String fullAccess = access.name;
                    switch (classTransform.removeFinal()) {
                        case ADD -> fullAccess = fullAccess + "+f";
                        case REMOVE -> fullAccess = fullAccess + "-f";
                    }

                    writer.write(fullAccess + " " + classTransform.className().replace("/", "."));
                    writer.write("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });


            this.fieldTransformers.forEach((fieldTransform, access) -> {
                try {
                    String fullAccess = access.name;
                    switch (fieldTransform.removeFinal()) {
                        case ADD -> fullAccess = fullAccess + "+f";
                        case REMOVE -> fullAccess = fullAccess + "-f";
                    }

                    writer.write(fullAccess + " " + fieldTransform.className().replace("/", ".") + " " + fieldTransform.field());
                    writer.write("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            this.methodTransformers.forEach((methodTransform, access) -> {
                try {
                    String fullAccess = access.name;
                    switch (methodTransform.removeFinal()) {
                        case ADD -> fullAccess = fullAccess + "+f";
                        case REMOVE -> fullAccess = fullAccess + "-f";
                    }

                    writer.write(fullAccess + " " + methodTransform.className().replace("/", ".") + " " + methodTransform.methodName() + methodTransform.methodSig());
                    writer.write("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
