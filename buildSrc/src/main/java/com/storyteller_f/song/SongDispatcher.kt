package com.storyteller_f.song

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

internal open class SongDispatcher : DefaultTask() {
    /**
     * 想要传送的文件
     */
    @InputFile
    lateinit var transferFiles: List<File>

    /**
     * adb 路径
     */
    @Input
    lateinit var adbPath: String

    /**
     * 普通目录
     */
    @Input
    lateinit var pathTargets: List<String>

    /**
     * /data/data/package-name/目录。这类目录无法直接通过push 传递。
     */
    @Input
    lateinit var packageTargets: List<Pair<String, String>>

    /**
     * /data/local/tmp 中的临时目录。
     */
    @Input
    lateinit var outputName: String

    @TaskAction
    fun dispatch() {
        logger.warn("dispatch start")
        extracted(transferFiles, packageTargets, pathTargets, adbPath, outputName)
    }

}