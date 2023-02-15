package com.storyteller_f.song

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.io.File

interface SongExtension {
    val transfers: ListProperty<String>
    val adb: Property<String>
    val paths: ListProperty<String>
    val packages: ListProperty<Pair<String, String>>
    val outputName: Property<String>
}

class Song : Plugin<Project> {
    override fun apply(target: Project) {
        val songExtension = target.extensions.create("song", SongExtension::class.java)

        val dispatchApk = target.tasks.register("dispatchApk", SongDispatcher::class.java) {
            it.transferFiles = songExtension.transfers.get().map { File(it) }
            it.adbPath = songExtension.adb.get()
            it.pathTargets = songExtension.paths.get()
            it.packageTargets = songExtension.packages.get()
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