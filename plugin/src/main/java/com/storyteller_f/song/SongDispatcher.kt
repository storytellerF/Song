package com.storyteller_f.song

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

internal open class SongDispatcher : DefaultTask() {
    /**
     * 想要传送的文件
     */
    @InputFiles
    var transferFiles: List<File>? = null

    /**
     * adb 路径
     */
    @Input
    var adbPath: String? = null

    /**
     * 普通目录
     */
    @Input
    var pathTargets: List<String>? = null

    /**
     * /data/data/package-name/目录。这类目录无法直接通过push 传递。
     */
    @Input
    var packageTargets: List<PackageSite>? = null

    /**
     * /data/local/tmp 中的临时目录。
     */
    @Input
    var outputName: String? = null

    @TaskAction
    fun dispatch() {
        logger.info("Dispatch start")
        SongAction(
            adbPath,
            outputName,
            transferFiles.orEmpty(),
            packageTargets.orEmpty(),
            pathTargets.orEmpty(),
            logger
        ).dispatchToMultiDevices()
        logger.info("Dispatch end")
    }

}