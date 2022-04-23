package me.hydos.focu;

import me.hydos.focu.mald.logic.MalderLoader;

import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        MalderLoader malderLoader = new MalderLoader()
                .loadMalder(Paths.get("malders/TitleScreen.class"));

        ForgePatcher patcher = new ForgePatcher(Paths.get("C:/Users/hayde/AppData/Roaming/.minecraft"), malderLoader, "1.18.2-40.0.52");

        patcher.apply();
    }
}
