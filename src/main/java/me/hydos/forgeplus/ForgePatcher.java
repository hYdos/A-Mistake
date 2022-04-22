package me.hydos.forgeplus;

import me.hydos.forgeplus.accesstransformer.AccessTransformer;
import me.hydos.forgeplus.mald.logic.MalderLoader;
import me.hydos.forgeplus.mald.logic.MalderPatcher;
import me.hydos.forgeplus.mald.logic.read.InjectTarget;
import me.hydos.forgeplus.util.FileSystemUtil;
import me.hydos.forgeplus.util.MappingUtils;
import net.fabricmc.tinyremapper.Main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@SuppressWarnings("ClassCanBeRecord")
public class ForgePatcher {
    public static final Path WORK_DIR = Paths.get(".work");
    public final Path minecraftDir;
    public final MalderLoader malderLoader;
    private final String forgeVersion;

    public ForgePatcher(Path minecraftDir, MalderLoader malderLoader, String forgeVersion) {
        this.minecraftDir = minecraftDir;
        this.malderLoader = malderLoader;
        this.forgeVersion = forgeVersion;

        try {
            Files.createDirectories(WORK_DIR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void remapToIntermediary() throws IOException {
        Path librariesDir = this.minecraftDir.resolve("libraries");
        Path forgeDir = librariesDir.resolve("net/minecraftforge/forge/" + forgeVersion);
        Path clientDir = librariesDir.resolve("net/minecraft/client/1.18.2-20220404.173914/");

        backupAndRemapJar(forgeDir.resolve("forge-" + forgeVersion + "-universal.jar"));
        backupAndRemapJar(clientDir.resolve("client-1.18.2-20220404.173914-srg.jar"));
    }

    public void applyMaldersToForgePatches() {
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
        }
    }
    private void backupAndRemapJar(Path inJar, Path outJar) throws IOException {
        Path srgToIntermediaryMappings = MappingUtils.getFullMappings();
        Path backupPath = inJar.getParent().resolve(inJar.getFileName().toString().replace(".jar", ".intermediary-backup.jar"));

        if (!Files.exists(backupPath)) {
            Files.copy(inJar, backupPath);
            Files.delete(inJar);

            Main.main(new String[]{
                    backupPath.toAbsolutePath().toString(),
                    outJar.toAbsolutePath().toString(),
                    srgToIntermediaryMappings.toAbsolutePath().toString(),
                    "srg",
                    "intermediary",
                    "--fixPackageAccess"
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
                                at.remap(MappingUtils.getFullMappings());
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
