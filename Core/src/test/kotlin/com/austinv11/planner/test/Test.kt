package com.austinv11.planner.test

import com.austinv11.planner.core.plugins.LocalPluginRepository
import com.austinv11.planner.core.plugins.RemotePluginRepository
import com.austinv11.planner.core.scripting.lua.LuaPluginLanguage
import com.google.gson.Gson
import java.io.File
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    //Tests scripting capabilities in lua:
    println("Compiling test script and luaj vm into memory...")
    val time = measureTimeMillis {
        val process = LuaPluginLanguage.execute(File(ClassLoader.getSystemResource("lua/test.lua").toURI()))
        process.callback = fun(wasInterrupted: Boolean, exception: Exception?) {
            exception?.printStackTrace()
        }
        process.start()
        while (!process.isFinished) {}
    }
    println("Compiled! (Took $time ms)")   
    
    //Tests plugin installation:
    println("Testing plugin installation...")
    val remoteRepo = RemotePluginRepository("https://raw.githubusercontent.com/austinv11/OpenPlanner/master/example/index.json")
    println(Gson().toJson(remoteRepo.metadata))
    remoteRepo.downloadPlugin(remoteRepo.plugins[0])
    println(Gson().toJson(LocalPluginRepository.plugins[0]))
    LocalPluginRepository.REPO_DIR.deleteOnExit()
}
