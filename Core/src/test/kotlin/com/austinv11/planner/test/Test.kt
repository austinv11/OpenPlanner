package com.austinv11.planner.test

import com.austinv11.planner.core.networking.Server
import com.austinv11.planner.core.plugins.LocalPluginRepository
import com.austinv11.planner.core.plugins.RemotePluginRepository
import com.austinv11.planner.core.scripting.lua.LuaPluginLanguage
import com.austinv11.planner.core.util.Security
import com.google.gson.Gson
import java.io.File
import java.util.*
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
    
    //Tests local plugin installations
    println(LocalPluginRepository.plugins[0])
    
    //Testing security
    val salt = Security.generateSalt()
    println("Salt: ${salt.string()}")
    val initialString = "Hello World"
    println("Initial string: $initialString")
    val hashed = Security.hash(initialString, salt)
    println("Hashed: ${hashed.string()}")
    val toCompare = "Blah"
    println("Comparison of $toCompare and ${hashed.string()}: ${Security.verify(hashed, toCompare, salt)}")
    println("Comparison of $initialString and ${hashed.string()}: ${Security.verify(hashed, initialString, salt)}")
    
    Server.start()
}

private fun ByteArray.string(): String {
    return Arrays.toString(this).replace("[", "").replace("]", "").replace(", ", "")
}
