package com.austinv11.planner.core.scripting.lua

import com.austinv11.planner.core.scripting.IPluginLanguage
import com.austinv11.planner.core.scripting.LanguageManager
import com.austinv11.planner.core.scripting.ScriptProcess
import org.luaj.vm2.lib.jse.JsePlatform

object LuaPluginLanguage : IPluginLanguage {
    
    private val additionalAPIS = mutableListOf<ILuaAPI>()
    
    override val extensions: Array<String> = arrayOf("lua", "luaj")
    
    override val name: String = "Lua"
    
    override val version: String = LanguageManager.API_SPEC_VERSION //Since this is a default/built-in language, this should always be up to date. If not, its a bug.

    init {
        
    }
    
    override fun execute(script: String): ScriptProcess { //TODO: Sandbox executions
        //NOTE: Objects are created dynamically to ensure that all scripts are independent of others.
        return ScriptProcess {
            val globals = JsePlatform.standardGlobals()
            ENVIRONMENT_VARIABLES.forEach { k, v ->
                globals.set(k, coerceWithTables(v))
            }
            additionalAPIS.forEach { 
                globals.load(it.toLuaLib())
            }
            val chunk = globals.load(script)
            val value = chunk.call()
        }
    }
    
    fun registerAPI(api: ILuaAPI) {
        additionalAPIS.add(api)
    }

    override fun toString() = name
}
