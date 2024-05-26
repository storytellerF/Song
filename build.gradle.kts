import com.storyteller_f.song.PackageSite
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    id("com.storyteller_f.song")
    application
}

group = "com.storyteller_f.song"
version = "1.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven { setUrl("./repo-snapshot") }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
val javaVersion = JavaVersion.VERSION_11
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = javaVersion.toString()
}

application {
    mainClass.set("MainKt")
}

val userHome: String = System.getProperty("user.home")
val file = "$userHome/AndroidStudioProjects/GiantExplorer/plugins/yue/app/build/outputs/apk/debug/app-debug.apk"

song {
    transfers.set(listOf(file))
    paths.set(listOf("/sdcard/Download/GiantExplorer/plugins"))
    packages.set(listOf(PackageSite("com.storyteller_f.giant_explorer.debug", "files/plugins")))
    outputName.set("yue.apk")
}