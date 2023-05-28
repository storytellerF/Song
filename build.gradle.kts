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

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

application {
    mainClass.set("MainKt")
}

val userHome: String = System.getProperty("user.home")
val file = "$userHome/AndroidStudioProjects/common-ui-list-structure/giant-explorer/yue/app/build/intermediates/apk/debug/app-debug.apk"

song {
    transfers.set(listOf(file))
    adb.set("$userHome/Library/Android/sdk/platform-tools/adb")
    paths.set(listOf())
    packages.set(listOf("com.storyteller_f.giant_explorer" to "files/plugins"))
    outputName.set("yue.apk")
}