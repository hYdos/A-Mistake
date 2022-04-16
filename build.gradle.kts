plugins {
    id("java")
}

group = "me.hydos"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.3")
    implementation("org.ow2.asm:asm-analysis:9.3")
    implementation("org.ow2.asm:asm-commons:9.3")
    implementation("org.ow2.asm:asm-tree:9.3")
    implementation("org.ow2.asm:asm-util:9.3")
}