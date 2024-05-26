package com.storyteller_f.song

import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger
import java.io.File
import java.io.Serializable
import java.util.regex.Pattern

private const val PROCESS_OK = 0

/**
 * @param path 相对路径
 */
data class PackageSite(val packageName: String, val path: String) : Serializable

/**
 * adbPath 最好添加到环境变量中，这样免去不同平台的适配
 * @param adbPath 如果为null 或者空字符串会通过ANDROID_HOME获取adb 的路径。不要带有exe 后缀，即便是在windows 环境中，内部会自动处理
 * @param pathTargets 绝对路径
 */
class SongAction(
    adbPath: String? = null,
    private val outputName: String? = null,
    private val transferFiles: List<File> = listOf(),
    private val packageTargets: List<PackageSite> = listOf(),
    private val pathTargets: List<String> = listOf(),
    private val logger: Logger = NOPLogger.NOP_LOGGER
) {
    private val safeAdbPath = if (adbPath != null && isWindows() && !adbPath.endsWith(".exe")) {
        "${adbPath}.exe"
    } else {
        adbPath
    }
    private val adb = when {
        safeAdbPath == "adb" -> "adb"
        safeAdbPath == "adb.exe" -> "adb.exe"
        safeAdbPath.isNullOrBlank() -> {
            val androidHome: String? = System.getenv("ANDROID_HOME")
            if (androidHome.isNullOrEmpty()) {
                null
            } else {
                val isWindows = isWindows()
                File(androidHome, "platform-tools/adb${if (isWindows) ".exe" else ""}").absolutePath
            }
        }

        File(safeAdbPath).exists() -> safeAdbPath
        else -> throw Exception("invalid adb path $adbPath, $safeAdbPath")
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name")?.startsWith("win", true) == true
    }

    fun dispatchToMultiDevices() {
        if (adb == null) {
            logger.warn("adbPath $safeAdbPath not exists. Skip!")
            return
        }

        val c = 1
        logger.info("${getSpace(c)}adb path is: $adb")
        val devices = getDevices(c)
        if (devices.isEmpty()) {
            logger.warn("${getSpace(c)}No connected devices. Skip.")
            return
        }
        transferFiles.filter { src ->
            if (src.exists()) true
            else {
                logger.warn("${getSpace(c)}$src not exists")
                false
            }
        }.forEach {
            dispatch(it, devices, c)
        }

    }

    @Suppress("SameParameterValue")
    private fun dispatch(it: File, devices: List<String>, c: Int) {
        val src = it.absolutePath
        val name = outputName ?: it.name
        devices.forEach { deviceSerialName ->
            logger.info("${getSpace(c)}Dispatch to $deviceSerialName")
            pathTargets.forEach { pathTarget ->
                logger.info("${getSpace(c + 1)}Dispatch to [pathTarget]: $pathTarget")
                command("Push to regular path", pushSimple(deviceSerialName, src, "$pathTarget/$name"), c + 1)
            }
            pushToInternal(deviceSerialName, src, name, c + 1)
        }
    }

    private fun pushToInternal(deviceSerialName: String, src: String, name: String, c: Int) {
        val tmp = "/data/local/tmp/${name}"
        if (command("${getSpace(c)}Push to temp", pushSimple(deviceSerialName, src, tmp), c) != PROCESS_OK) {
            return
        }
        packageTargets.forEach { (packageName, subPath) ->
            val outputPath = "/data/data/$packageName/$subPath"
            logger.info("${getSpace(c)}Dispatch to [packageTarget]: $outputPath")
            val output = "$outputPath/${outputName}"
            if (checkPackageExists(packageName, deviceSerialName, c)) {
                sequence {
                    yield(
                        command(
                            "Mkdirs app internal package",
                            mkdirsInInternal(deviceSerialName, packageName, outputPath),
                            c + 1
                        )
                    )
                    yield(
                        command(
                            "Push to app internal package",
                            copyToInternal(deviceSerialName, packageName, tmp, output),
                            c + 1
                        )
                    )
                }.any {
                    //有任何一个返回了非零的结果，都会退出
                    it != PROCESS_OK
                }
            } else {
                logger.info("${getSpace(c + 1)}$packageName not exists, Skip!")
            }


        }
        command("Remove temp", removeTemp(deviceSerialName, tmp), c)
    }

    /**
     * @return true 代表存在
     */
    private fun checkPackageExists(packageName: String, deviceSerial: String, c: Int): Boolean {
        if (isWindows()) {
            val commandResult =
                asyncResult(
                    "checkPackageExists",
                    arrayOf(
                        "powershell.exe",
                        "-c",
                        """$adb -s $deviceSerial shell pm list package | Select-String "$packageName$""""
                    ),
                    c
                )
            return commandResult.contains(packageName)
        } else {
            return commandResult(
                "Check $packageName exists", arrayOf(
                    "/bin/sh",
                    "-c",
                    """$adb -s $deviceSerial shell pm list package | grep  "$packageName$""""
                ),
                c
            ).isNotEmpty()
        }
    }

    private fun pushSimple(
        deviceSerialName: String,
        src: String,
        path: String
    ) = arrayOf(
        adb!!,
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
        adb!!,
        "-s",
        deviceSerial,
        "shell",
        "sh",
        "-c",
        "'rm $tmp'"
    )

    @Suppress("SameParameterValue")
    private fun getDevices(c: Int): List<String> {
        val text = commandResult("Get Devices", arrayOf(adb!!, "devices"), c)
        return text.split("\n").let {
            it.subList(1, it.size)
        }.mapNotNull {
            val split = it.split(Pattern.compile("\\s+"))
            val device = split.first()
            if (split[1] == "offline") {
                logger.info("${getSpace(c)}Skip device $device, because of offline")
                null
            } else {
                device
            }
        }
    }

    private fun copyToInternal(
        deviceSerial: String,
        packageTarget: String,
        tmp: String,
        output: String
    ) = arrayOf(
        adb!!,
        "-s",
        deviceSerial,
        "shell",
        "run-as",
        packageTarget,
        "sh",
        "-c",
        "'/bin/cp -f $tmp $output'"
    )

    private fun mkdirsInInternal(
        deviceSerial: String,
        packageTarget: String,
        outputPath: String,
    ) = arrayOf(
        adb!!,
        "-s",
        deviceSerial,
        "shell",
        "run-as",
        packageTarget,
        "sh",
        "-c",
        "'mkdir -p $outputPath'"
    )

    /**
     * 如果执行失败，会显示命令输出的内容
     * @return 返回命令结果
     */
    private fun command(label: String, commands: Array<String>, c: Int): Int {
        val pushCommand = Runtime.getRuntime().exec(commands)
        val processResult = pushCommand.waitFor()
        val inputText = pushCommand.inputStream.reader().readText()
        val errorText = pushCommand.errorStream.reader().readText()
        if (processResult != PROCESS_OK)
            logCommandError(label, commands, processResult, inputText, errorText, c)
        pushCommand.destroy()
        return processResult
    }

    private fun commandResult(label: String, commands: Array<String>, c: Int): String {
        val process = Runtime.getRuntime().exec(commands)
        val result = process.waitFor()
        val inputContent = process.inputStream.bufferedReader().use {
            it.readText().trim()
        }
        val errorContent = process.errorStream.bufferedReader().use {
            it.readText().trim()
        }
        if (result != PROCESS_OK) {
            logCommandError(label, commands, result, inputContent, errorContent, c)
        }
        process.destroy()
        return inputContent
    }

    private fun logCommandError(
        label: String,
        commands: Array<String>,
        result: Int,
        inputContent: String,
        errorContent: String,
        c: Int
    ) {
        logger.error(
            buildString {
                append("${getSpace(c)}$label\n")
                append("${getSpace(c + 1)}command: ${commands.joinToString()}\n")
                append("${getSpace(c + 1)}result code: $result")
                if (inputContent.isNotBlank())
                    append("\n${getSpace(c + 1)}input: ${inputContent.trim()}")
                if (errorContent.isNotBlank())
                    append("\n${getSpace(c + 1)}error: ${errorContent.trim()}")
            }
        )
    }

    @Suppress("SameParameterValue")
    private fun asyncResult(label: String, commands: Array<String>, c: Int): String {
        val process = Runtime.getRuntime().exec(commands)
        val inputBuffer = StringBuilder()
        val errorBuffer = StringBuilder()
        val i = process.inputStream.bufferedReader()
        val e = process.errorStream.bufferedReader()
        val thread = Thread {
            while (true) {
                if (process.isAlive) {
                    inputBuffer.appendLine(i.readLine())
                } else {
                    inputBuffer.appendLine(i.readText())
                    break
                }
            }
        }
        thread.start()
        val thread1 = Thread {
            while (true) {
                if (process.isAlive) {
                    errorBuffer.appendLine(e.readLine())
                } else {
                    errorBuffer.appendLine(e.readText())
                    break
                }
            }
        }
        thread1.start()
        thread.join()
        thread1.join()
        val result = process.waitFor()
        val inputContent = inputBuffer.toString()
        val errorContent = errorBuffer.toString()

        if (result != PROCESS_OK) {
            logCommandError(label, commands, result, inputContent, errorContent, c)
        }
        process.destroy()
        i.close()
        e.close()
        return inputContent
    }

    private fun getSpace(count: Int): String {
        return MutableList(count) {
            "\t"
        }.joinToString("")
    }
}

