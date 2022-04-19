package me.hydos.forgeplus.accesstransformer;

public record MethodTransform(String className, String methodName, String methodSig, FinalModifier removeFinal) {
}
