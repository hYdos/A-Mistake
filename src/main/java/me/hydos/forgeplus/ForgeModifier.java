package me.hydos.forgeplus;

import me.hydos.forgeplus.mald.logic.MalderLoader;
import me.hydos.forgeplus.mald.logic.MalderPatcher;
import me.hydos.forgeplus.mald.logic.read.InjectTarget;
import me.hydos.forgeplus.util.FileSystemUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@SuppressWarnings("ClassCanBeRecord")
public class ForgeModifier {

    public final Path minecraftDir;
    public final MalderLoader malderLoader;

    public ForgeModifier(Path minecraftDir, MalderLoader malderLoader) {
        this.minecraftDir = minecraftDir;
        this.malderLoader = malderLoader;
    }

    public void applyMalders() {
        Path librariesDir = this.minecraftDir.resolve("libraries");
        Path forgePatches = librariesDir.resolve("net/minecraftforge/forge/1.18.2-40.0.52/forge-1.18.2-40.0.52-client.jar");
        Path backupForgePatches = librariesDir.resolve("net/minecraftforge/forge/1.18.2-40.0.52/forge-1.18.2-40.0.52-client.jar.backup");

        try {
            if (!Files.exists(backupForgePatches)) {
                Files.copy(forgePatches, backupForgePatches);
            }

            Files.delete(forgePatches);
        } catch (IOException e) {
            throw new RuntimeException("Failed to backup vanilla forge!", e);
        }

        Path workDir = forgePatches.getParent().resolve(".work");
        try {
            Files.createDirectories(workDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (FileSystemUtil.Delegate output = FileSystemUtil.getJarFileSystem(forgePatches, true)) {
            try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(backupForgePatches); Stream<Path> classes = Files.walk(fs.get().getPath("/"))) {
                classes.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        byte[] bytes = Files.readAllBytes(path);
                        byte[] modifiedBytes = applyPatches(path, bytes);

                        if (bytes.length != modifiedBytes.length) {
                            System.out.println("Outputting " + path.getFileName().toString() + " for debugging");
                            Path classFile = workDir.resolve(path.getFileName().toString());
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
