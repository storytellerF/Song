import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    id("com.storyteller_f.song")
    application
}

group = "com.storyteller_f.song"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
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
val file = "$userHome/AndroidStudioProjects/common-ui-list-structure/giant-explorer/yue/app/build/outputs/apk/debug/app-debug.apk"

song {
    this.apkFile.set(file)
    this.adb.set("$userHome/Library/Android/sdk/platform-tools/adb")
    this.paths.set(listOf())
    this.packages.set(listOf("com.storyteller_f.giant_explorer" to "files/plugins"))
    this.outputName.set("yue.apk")
}