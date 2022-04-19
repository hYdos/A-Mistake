package me.hydos.forgeplus;

import me.hydos.forgeplus.mald.logic.MalderLoader;

import java.io.IOException;
import java.nio.file.Paths;

public class ForgePlus {

    public static void main(String[] args) {
        MalderLoader malderLoader = new MalderLoader();
        try {
            malderLoader.loadMalder(Paths.get("malders/TitleScreen.class"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load malders", e);
        }

        ForgePatcher patcher = new ForgePatcher(Paths.get("C:/Users/hayde/AppData/Roaming/.minecraft"), malderLoader, "1.18.2-40.0.52");

        try {
            patcher.remapToIntermediary();
        } catch (IOException e) {
            throw new RuntimeException("Failed to remap to intermediary.", e);
        }
        patcher.applyMaldersToForgePatches();
    }
}
