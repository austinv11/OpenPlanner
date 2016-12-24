package com.austinv11.planner.core.scripting

import java.io.File

/**
 * This interface represents a plugin-compatible scripting language.
 * NOTE: These evaluation classes should be able to be called concurrently in separate threads,
 */
interface IPluginLanguage {

    /**
     * This represents the file extensions which is usable by this language (does NOT include the period in the file 
     * extension).
     */
    val extensions: Array<String>

    /**
     * This represents the name of the language.
     */
    val name: String

    /**
     * This represents the version of the api spec this interface was built on.
     */
    val version: String

    /**
     * This function is called to execute a script. 
     * @param script The script to be executed.
     * @return The script process.
     */
    fun execute(script: String): ScriptProcess

    /**
     * This function is called to execute a script.
     * @param script The script to be executed.
     * @return The script process.
    */
    fun execute(script: File): ScriptProcess = execute(script.readText())
}
