package me.hydos.focu.accesstransformer;

public record MethodTransform(String className, String methodName, String methodSig, FinalModifier removeFinal) {
}
