package com.storyteller_f.song

import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger
import java.io.File
import java.util.regex.Pattern

private const val PROCESS_OK = 0

/**
 * @param path 相对路径
 */
data class PackageSite(val packageName: String, val path: String)

/**
 * @param pathTargets 绝对路径
 */
class SongAction(
    private val transferFiles: List<File> = listOf(),
    private val packageTargets: List<PackageSite> = listOf(),
    private val pathTargets: List<String> = listOf(),
    private val adbPath: String = "adb",
    private val outputName: String = "temp.file",
    private val logger: Logger = NOPLogger.NOP_LOGGER
) {
    fun dispatchToMultiDevices() {
        if (!File(adbPath).exists()) {
            logger.warn("adbPath $adbPath not exists. Skip!")
            return
        }
        val tmp = "/data/local/tmp/${outputName}"
        val devices = getDevices()
        transferFiles.filter { src ->
            if (src.exists()) true
            else {
                logger.warn("$src not exists")
                false
            }
        }.forEach {
            val src = it.absolutePath
            devices.forEach { deviceSerialName ->
                logger.info("Dispatch to $deviceSerialName")
                pathTargets.forEach { pathTarget ->
                    logger.info("\tDispatch to [pathTarget]: $pathTarget")
                    command("Push to regular path", pushSimple(deviceSerialName, src, pathTarget))
                }
                pushToInternal(deviceSerialName, src, tmp)
            }
        }

    }

    private fun pushToInternal(deviceSerialName: String, src: String, tmp: String) {
        if (command("Push to temp", pushSimple(deviceSerialName, src, tmp)) != PROCESS_OK) {
            return
        }
        packageTargets.forEach { (packageName, subPath) ->
            val outputPath = "/data/data/$packageName/$subPath"
            logger.info("\tDispatch to [packageTarget]: $outputPath")
            val output = "$outputPath/${outputName}"

            sequence {
                yield(
                    command(
                        "Mkdirs app internal package",
                        mkdirsInInternal(deviceSerialName, packageName, outputPath)
                    )
                )
                yield(
                    command(
                        "Push to app internal package",
                        pushToInternal(deviceSerialName, packageName, tmp, output)
                    )
                )
            }.any {
                //有任何一个返回了非零的结果，都会退出
                it != PROCESS_OK
            }

        }
        command("Remove temp", removeTemp(deviceSerialName, tmp))
    }

    private fun pushSimple(
        deviceSerialName: String,
        src: String,
        path: String
    ) = arrayOf(
        adbPath,
        "-s",
        deviceSerialName,
        "push",
        src,
        path
    )

    private fun removeTemp(
        deviceSerial: String,
        tmp: String
    ) = arrayOf(
        adbPath,
        "-s",
        deviceSerial,
        "shell",
        "sh",
        "-c",
        "\'rm $tmp\'"
    )

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
    ) = arrayOf(
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

    private fun mkdirsInInternal(
        deviceSerial: String,
        packageTarget: String,
        outputPath: String,
    ) = arrayOf(
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

    /**
     * @return 返回命令结果
     */
    private fun command(label: String, arrayOf: Array<String>): Int {
        val pushCommand = Runtime.getRuntime().exec(arrayOf)
        val processResult = pushCommand.waitFor()
        val readText = pushCommand.inputStream.reader().readText()
        val error = pushCommand.errorStream.reader().readText()
        if (processResult != PROCESS_OK)
            logger.warn("$label command result: $processResult input: $readText error: $error")
        pushCommand.destroy()
        return processResult
    }
}

