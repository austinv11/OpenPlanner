package com.austinv11.planner.core.scripting

import com.austinv11.planner.core.scripting.lua.LuaPluginLanguage
import java.io.File

/**
 * This class manages the set of languages available to plugins.
 */
object LanguageManager {
    
    /**
     * This is the set of available languages for plugins.
     */
    val LANGUAGES = mutableSetOf<IPluginLanguage>()

    /**
     * This represents the api specification version which is used by this version of OpenPlanner-Core.
     */
    const val API_SPEC_VERSION = "1.0"

    init { //Registers included plugin languages.
        LANGUAGES.add(LuaPluginLanguage)
    }

    /**
     * This gets the appropriate [IPluginLanguage] for a filename.
     * @return The language for this filename, or null if no suitable language was found.
     */
    fun getLanguageForFile(fileName: String): IPluginLanguage? {
        return LANGUAGES.find { it.extensions.contains(fileName.split(".")[1]) }
    }

    /**
     * This executes the script located in a file.
     * @return The script process for the file if an applicable language was found.
     */
    fun execute(file: File): ScriptProcess? {
        return getLanguageForFile(file.name)?.execute(file)
    }
}
