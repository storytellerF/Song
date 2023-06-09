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

        target.tasks.register(taskName, SongDispatcher::class.java) {
            group = "distribution"
            transferFiles = songExtension.transfers.get().map { File(it) }
            adbPath = songExtension.adb.get()
            pathTargets = songExtension.paths.get()
            packageTargets = songExtension.packages.get()
            outputName = songExtension.outputName.get()
        }
    }
    companion object {
        const val taskName = "song"
    }
}