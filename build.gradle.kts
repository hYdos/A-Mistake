plugins {
    id("java")
}

group = "me.hydos"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://maven.fabricmc.net")
    maven("https://maven.minecraftforge.net")
    mavenCentral()
}

dependencies {
    implementation("net.fabricmc:tiny-remapper:0.8.1")
    implementation("net.fabricmc:mapping-io:0.3.0")
    implementation ("net.minecraftforge:installertools:1.2.10")

    implementation("org.ow2.asm:asm:9.3")
    implementation("org.ow2.asm:asm-analysis:9.3")
    implementation("org.ow2.asm:asm-commons:9.3")
    implementation("org.ow2.asm:asm-tree:9.3")
    implementation("org.ow2.asm:asm-util:9.3")
}