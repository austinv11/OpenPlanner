package com.austinv11.planner.launch

import com.austinv11.planner.launch.ExitCodes.Companion.MISSING_RESOURCES
import com.austinv11.planner.launch.ExitCodes.Companion.NORMAL
import com.github.kittinunf.fuel.httpDownload
import java.io.File
import kotlin.system.exitProcess

const val LAUNCHER_VERSION = "1.0.0-SNAPSHOT"
const val DEFAULT_INSTALL_LOCATION = "./installation/OpenPlanner-Core.jar"
const val DOWNLOAD_URL = "" //TODO change to real url

fun main(vararg args: String) {
    println("OpenPlanner Server Launcher v$LAUNCHER_VERSION")

    val dir: File

    if (args.isNotEmpty() && File(args[0]).exists()) {
        println("Using custom install directory (${args[0]})...")
        dir = File(args[0])
    } else {
        println("Using default install directory ($DEFAULT_INSTALL_LOCATION)...")
        dir = File(DEFAULT_INSTALL_LOCATION)
    }

    if (!dir.exists()) {
        if (dir.mkdirs()) dir.delete() else exitProcess(MISSING_RESOURCES)

        val (request, response, result) = DOWNLOAD_URL.httpDownload().destination { response, url ->
            return@destination dir
        }.responseString()
        result.fold({
            println("Downloaded necessary resources successfully!")
        }, {
            println("Unable to download necessary resources! Shutting down...")
            exitProcess(MISSING_RESOURCES)
        })
    }

    if (!dir.exists() || dir.isDirectory || dir.extension != "jar") {
        println("ERROR! Invalid file location.")
        exitProcess(MISSING_RESOURCES)
    }

    var process: Process
    var exitCode: Int = -1
    while (exitCode != NORMAL) {
        process = ProcessBuilder(listOf("java", "-jar", dir.absolutePath)).inheritIO().start()
        exitCode = process.waitFor()

        if (exitCode == NORMAL)
            exitProcess(exitCode)

        println("OpenPlanner has shutdown unexpectedly! Attempting to restart...")
    }
}

class ExitCodes {
    companion object {
        const val NORMAL = 0
        const val MISSING_RESOURCES = 1
    }
}