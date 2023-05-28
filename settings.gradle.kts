pluginManagement {
    includeBuild("plugin")

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { setUrl("./repo-snapshot") }
    }
}


rootProject.name = "song"
