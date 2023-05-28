package com.storyteller_f.song

import org.gradle.api.logging.Logger
import java.io.File
import java.util.regex.Pattern

class SongAction(
    private val transferFiles: List<File>,
    private val packageTargets: List<Pair<String, String>>,
    private val pathTargets: List<String>,
    private val adbPath: String,
    private val outputName: String,
    private val logger: Logger
) {
    fun dispatchToMultiDevices() {
        if (!File(adbPath).exists()) {
            logger.warn("adbPath $adbPath not exists. Skip!")
            return
        }
        val tmp = "/data/local/tmp/${outputName}"
        val devices = getDevices()
        transferFiles.forEach {
            val src = it.absolutePath
            if (!File(src).exists()) {
                logger.warn("$src not exists")
                return@forEach
            }
            devices.forEach { deviceSerial ->
                logger.info("dispatch to $deviceSerial")
                pathTargets.forEach { pathTarget ->
                    logger.info("\tdispatch to path: $pathTarget")
                    command(pushSimple(deviceSerial, src, pathTarget), "push to regular path")
                }
                pushToInternal(deviceSerial, src, tmp)
            }
        }

    }

    private fun pushToInternal(deviceSerial: String, src: String, tmp: String) {
        command(pushSimple(deviceSerial, src, tmp), "push to temp")
        packageTargets.forEach { (packageTarget, sp) ->
            val outputPath = "/data/data/$packageTarget/$sp"
            logger.info("\tdispatch to pk: $outputPath")
            val output = "$outputPath/${outputName}"
            command(
                mkdirsInInternal(deviceSerial, packageTarget, outputPath),
                "mkdirs app internal package"
            )
            command(
                pushToInternal(deviceSerial, packageTarget, tmp, output),
                "push to app internal package"
            )
        }
        command(removeTemp(deviceSerial, tmp), "remove temp")
    }

    private fun pushSimple(
        deviceSerial: String,
        src: String,
        path: String
    ) = arrayOf(adbPath, "-s", deviceSerial, "push", src, path)

    private fun removeTemp(
        deviceSerial: String,
        tmp: String
    ) = arrayOf(adbPath, "-s", deviceSerial, "shell", "sh", "-c", "\'rm $tmp\'")

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

    private fun pushToInternal(
        deviceSerial: String,
        packageTarget: String,
        tmp: String,
        output: String
    ): Array<String> {
        return arrayOf(
            adbPath,
            "-s",
            deviceSerial,
            "shell",
            "run-as",
            packageTarget,
            "sh",
            "-c",
            "\'/bin/cp -f $tmp $output\'"
        )
    }

    private fun mkdirsInInternal(
        deviceSerial: String,
        packageTarget: String,
        outputPath: String,
    ): Array<String> {
        return arrayOf(
            adbPath,
            "-s",
            deviceSerial,
            "shell",
            "run-as",
            packageTarget,
            "sh",
            "-c",
            "\'mkdir -p $outputPath\'"
        )
    }

    private fun command(arrayOf: Array<String>, label: String): Int {
        val pushCommand = Runtime.getRuntime().exec(arrayOf)
        val waitFor = pushCommand.waitFor()
        val readText = pushCommand.inputStream.reader().readText()
        val error = pushCommand.errorStream.reader().readText()
        if (waitFor != 0)
            logger.warn("$label command result: $waitFor input: $readText error: $error")
        pushCommand.destroy()
        return waitFor
    }
}

