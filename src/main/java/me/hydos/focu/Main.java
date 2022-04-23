package me.hydos.focu;

import me.hydos.focu.mald.logic.MalderLoader;
import me.hydos.focu.providers.ForgeProvider;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        ForgeProvider forge = ForgeProvider.createFromClient(
                Path.of("C:/Users/hayde/AppData/Roaming/.minecraft"),
                "1.18.2",
                "40.1.0"
        );

        MalderLoader malderLoader = new MalderLoader();
                //.loadMalder(Paths.get("malders/TitleScreen.class"));

        ForgePatcher patcher = new ForgePatcher(forge, malderLoader);
        patcher.apply();
    }
}
