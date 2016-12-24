package com.austinv11.planner.test

import com.austinv11.planner.core.scripting.lua.LuaPluginLanguage
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
}
