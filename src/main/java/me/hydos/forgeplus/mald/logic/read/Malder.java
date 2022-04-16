package me.hydos.forgeplus.mald.logic.read;

import java.util.ArrayList;
import java.util.List;

public class Malder {

    public String targetClass;
    public List<String> targetMethods;
    public byte[] bytecode;

    public Malder() {
        this.targetMethods = new ArrayList<>();
    }
}
