package com.storyteller_f.song

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.io.File

interface SongExtension {
    val apkFile: Property<String>
    val adb: Property<String>
    val paths: ListProperty<String>
    val packages: ListProperty<Pair<String, String>>
    val outputName: Property<String>
}

@Suppress("unused")
class Song : Plugin<Project> {
    override fun apply(target: Project) {
        val songExtension = target.extensions.create("song", SongExtension::class.java)

        val dispatchApk = target.tasks.register("dispatchApk", SongDispatcher::class.java) {
            it.apkFile = File(songExtension.apkFile.get())
            it.adbPath = songExtension.adb.get()
            it.paths = songExtension.paths.get()
            it.packages = songExtension.packages.get()
            it.outputName = songExtension.outputName.get()
        }
        target.afterEvaluate {
            val taskName = "packageDebug"
            it.tasks.findByName(taskName)?.let { packageDebug ->
                dispatchApk.get().dependsOn(taskName)
                packageDebug.finalizedBy(dispatchApk)
            }

        }
    }
}