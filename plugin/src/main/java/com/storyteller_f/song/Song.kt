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
    val packages: ListProperty<PackageSite>
    val outputName: Property<String>
}

class Song : Plugin<Project> {
    override fun apply(target: Project) {
        val songExtension = target.extensions.create("song", SongExtension::class.java)

        target.tasks.register(TASK_NAME, SongDispatcher::class.java) {
            group = "distribution"
            transferFiles = songExtension.transfers.getOrElse(emptyList()).map { File(it) }
            adbPath = songExtension.adb.orNull
            pathTargets = songExtension.paths.orNull
            packageTargets = songExtension.packages.orNull
            outputName = songExtension.outputName.orNull
            outputs.upToDateWhen {
                false
            }
        }
    }
    companion object {
        const val TASK_NAME = "song"
    }
}