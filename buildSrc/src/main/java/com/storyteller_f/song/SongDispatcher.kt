package com.storyteller_f.song

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.regex.Pattern

internal open class SongDispatcher : DefaultTask() {
    @InputFile
    lateinit var apkFile: File

    @Input
    lateinit var adbPath: String

    @Input
    lateinit var paths: List<String>

    @Input
    lateinit var packages: List<Pair<String, String>>

    @Input
    lateinit var outputName: String

    @TaskAction
    fun dispatch() {
        logger.warn("dispatch start")
        val tmp = "/data/local/tmp/$outputName"
        val devices = getDevices()
        val src = apkFile.absolutePath
        devices.forEach { deviceSerial ->
            println("dispatch to $deviceSerial")
            paths.forEach {
                command(arrayOf(adbPath, "-s", deviceSerial, "push", src, it))
            }
            command(arrayOf(adbPath, "-s", deviceSerial, "push", src, tmp))
            packages.forEach {(pn, sp) ->
                val outputPath = "/data/data/$pn/$sp"
                val output = "$outputPath/$outputName"
                command(createOutputCommand(deviceSerial, pn, outputPath))
                command(createCopyCommand(deviceSerial, pn, tmp, output))
            }
            command(arrayOf(adbPath, "-s", deviceSerial, "shell", "sh", "-c", "\'rm $tmp\'"))
        }
    }

    private fun getDevices(): List<String> {
        val getDevicesCommand = Runtime.getRuntime().exec(arrayOf(adbPath, "devices"))
        getDevicesCommand.waitFor()
        val readText = getDevicesCommand.inputStream.bufferedReader().use {
            it.readText().trim()
        }
        getDevicesCommand.destroy()
        val devices = readText.split("\n").let {
            it.subList(1, it.size)
        }.map {
            it.split(Pattern.compile("\\s+")).first()
        }
        return devices
    }

    private fun createCopyCommand(
        deviceSerial: String,
        pn: String,
        tmp: String,
        output: String
    ): Array<String> {
        return arrayOf(
            adbPath,
            "-s",
            deviceSerial,
            "shell",
            "run-as",
            pn,
            "sh",
            "-c",
            "\'cp $tmp $output\'"
        )
    }

    private fun createOutputCommand(
        deviceSerial: String,
        pn: String,
        outputPath: String
    ): Array<String> {
        return arrayOf(
            adbPath,
            "-s",
            deviceSerial,
            "shell",
            "run-as",
            pn,
            "sh",
            "-c",
            "\'mkdir $outputPath\'"
        )
    }

    private fun command(arrayOf: Array<String>): Int {
        val pushCommand = Runtime.getRuntime().exec(arrayOf)
        val waitFor = pushCommand.waitFor()
        pushCommand.destroy()
        return waitFor
    }

}