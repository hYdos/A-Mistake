package me.hydos.focu.providers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public record ForgeProvider(
        List<Path> targets,
        Path mojmap,
        Path minecraftSrg
) {

    public static ForgeProvider createFromClient(Path dotMinecraftDir, String minecraftVersion, String forgeVersion) {
        String fullForgeVersion = minecraftVersion + "-" + forgeVersion;
        String fullMinecraftVersion = detectFullMinecraftVersion(minecraftVersion, dotMinecraftDir);
        Path librariesFolder = dotMinecraftDir.resolve("libraries");
        Path forgeOrgFolder = librariesFolder.resolve("net/minecraftforge/forge/" + fullForgeVersion);
        Path minecraftOrgFolder = librariesFolder.resolve("net/minecraft/client/" + fullMinecraftVersion);

        Path mcSrg = minecraftOrgFolder.resolve("client-" + fullMinecraftVersion + "-srg.jar");
        return new ForgeProvider(List.of(
                forgeOrgFolder.resolve("forge-" + fullForgeVersion + "-client.jar"),
                forgeOrgFolder.resolve("forge-" + fullForgeVersion + "-universal.jar"),
                mcSrg
        ), minecraftOrgFolder, mcSrg);
    }

    private static String detectFullMinecraftVersion(String minecraftVersion, Path dotMinecraftDir) {
        try {
            try (Stream<Path> list = Files.list(dotMinecraftDir.resolve("libraries/net/minecraft/client"))) {
                List<Path> results = list.filter(Files::isDirectory).filter(path -> path.toString().contains(minecraftVersion)).toList();
                if (results.isEmpty()) {
                    throw new RuntimeException("Forge Installer has not been run!");
                } else if (results.size() > 1) {
                    System.out.println("More than one version of " + minecraftVersion + ". Patcher may not work!");
                }

                return minecraftVersion + results.get(0).getFileName().toString().substring(minecraftVersion.length());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to detect full minecraft version", e);
        }
    }
}
