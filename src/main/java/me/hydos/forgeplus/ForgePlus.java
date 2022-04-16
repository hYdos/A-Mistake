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

        new ForgeModifier(Paths.get("C:/Users/hayde/AppData/Roaming/.minecraft"), malderLoader)
                .applyMalders();
    }
}
