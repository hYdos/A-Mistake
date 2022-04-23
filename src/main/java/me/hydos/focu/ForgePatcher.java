package me.hydos.focu;

import me.hydos.focu.accesstransformer.AccessTransformer;
import me.hydos.focu.mald.logic.MalderLoader;
import me.hydos.focu.mald.logic.MalderPatcher;
import me.hydos.focu.mald.logic.read.InjectTarget;
import me.hydos.focu.providers.ForgeProvider;
import me.hydos.focu.util.FileSystemUtil;
import me.hydos.focu.util.MappingUtils;
import net.fabricmc.tinyremapper.Main;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@SuppressWarnings("ClassCanBeRecord")
public class ForgePatcher {
    public static final int ASM_VERSION = Opcodes.ASM9;
    public static final Path WORK_DIR = Paths.get(".work");
    public final ForgeProvider forge;
    public final MalderLoader malderLoader;

    public ForgePatcher(ForgeProvider forge, MalderLoader malderLoader) {
        this.forge = forge;
        this.malderLoader = malderLoader;

        try {
            Files.createDirectories(WORK_DIR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void remapToIntermediary() throws IOException {
        for (Path target : this.forge.targets()) {
            System.out.println("Remapping " + target.getFileName().toString());
            backupAndRemapJar(target);
        }
    }

    public void apply() {
        try {
            remapToIntermediary();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
/*
        Path librariesDir = this.minecraftDir.resolve("libraries");
        Path forgePatches = librariesDir.resolve("net/minecraftforge/forge/1.18.2-40.0.52/forge-1.18.2-40.0.52-client.jar");
        Path forgeIntermediary = forgePatches.getParent().resolve("forge-intermediary.jar");
        try {
            backupAndRemapJar(forgePatches, forgeIntermediary);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        try (FileSystemUtil.Delegate output = FileSystemUtil.getJarFileSystem(forgePatches, true)) {
            try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(forgeIntermediary); Stream<Path> classes = Files.walk(fs.get().getPath("/"))) {
                classes.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        byte[] bytes = Files.readAllBytes(path);
                        byte[] modifiedBytes = applyPatches(path, bytes);

                        if (bytes.length != modifiedBytes.length) {
                            System.out.println("Outputting " + path.getFileName().toString() + " for debugging");
                            Path classFile = WORK_DIR.resolve(path.getFileName().toString());
                            Files.deleteIfExists(classFile);
                            Files.write(classFile, modifiedBytes);
                        }

                        Path outPath = moveFilesystems(path, output);
                        Files.deleteIfExists(outPath);
                        Files.createDirectories(outPath.getParent());
                        Files.write(outPath, modifiedBytes);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read class bytes.", e);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("Failed to read forge patches jar!", e);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output jar!", e);
        }*/
    }

    private void backupAndRemapJar(Path inJar, Path outJar) throws IOException {
        Path srgToIntermediaryMappings = MappingUtils.getFullMappings(this.forge.minecraftSrg());
        Path backupPath = inJar.getParent().resolve(inJar.getFileName().toString().replace(".jar", ".intermediary-backup.jar"));

        if (!Files.exists(backupPath)) {
            Files.copy(inJar, backupPath);
            Files.delete(inJar);

            Main.main(new String[]{
                    backupPath.toAbsolutePath().toString(),
                    outJar.toAbsolutePath().toString(),
                    srgToIntermediaryMappings.toAbsolutePath().toString(),
                    "srg",
                    "intermediary"
            });

            try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(outJar); Stream<Path> files = Files.walk(fs.get().getPath("/"))) {
                files.filter(path -> path.toString().contains("META-INF")).forEach(path -> {
                    try {
                        String fileName = path.getFileName().toString();
                        if (fileName.endsWith(".RSA") || fileName.endsWith(".SF")) {
                            // Remove Forge's signing because we are no longer a vanilla forge jar
                            Files.delete(path);
                        } else {
                            if (fileName.endsWith(".cfg")) {
                                // Hope it's an AccessTransformer
                                AccessTransformer at = AccessTransformer.read(path);
                                System.out.println("Remapping Access Widener");
                                at.remap(srgToIntermediaryMappings);
                                System.out.println("Overwriting old Access Widener");
                                Files.delete(path);
                                at.write(path);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

    }

    private void backupAndRemapJar(Path jar) throws IOException {
        backupAndRemapJar(jar, jar);
    }

    private Path moveFilesystems(Path path, FileSystemUtil.Delegate output) {
        return output.get().getPath(path.toString());
    }

    private byte[] applyPatches(Path file, byte[] bytes) {
        String className = file.toString().replace("/", ".").replace(".class", "").substring(1);
        System.out.println("Patching " + className + " ...");

        MalderPatcher patcher = this.malderLoader.getPatcher(className);
        if (patcher != null) {
            return patcher.patch(bytes, InjectTarget.END);
        }
        return bytes;
    }
}
