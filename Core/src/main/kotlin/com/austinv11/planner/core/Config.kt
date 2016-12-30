package com.austinv11.planner.core

import com.google.gson.GsonBuilder
import java.io.File
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.memberProperties

object Config : IConfig {

    val FILE = File("./config.json")
    private val GSON = GsonBuilder().setPrettyPrinting().serializeNulls().create()
    private var _backing = BackingConfigObject()
    
    init {
        if (FILE.exists()) {
            _backing = GSON.fromJson(FILE.readText(), BackingConfigObject::class.java)
        }
            
        save()
    }
    
    fun save() {
        val writer = FILE.writer()
        GSON.toJson(_backing, writer)
        writer.close()
    }
    
    override var port: Int by ConfigDelegate<Int>()
    
    private class ConfigDelegate<T> {
        
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            thisRef as Config
            val field = thisRef._backing.javaClass.kotlin.memberProperties.find { it.name == property.name }!!
            field.isAccessible = true
            return field.get(thisRef._backing) as T
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            thisRef as Config
            val field = thisRef._backing.javaClass.kotlin.memberProperties.find { it.name == property.name }!!.javaField!!
            field.isAccessible = true
            field.set(thisRef._backing, value)
            save()
        }
    }
    
    private data class BackingConfigObject(override var port: Int = 3000) : IConfig
}

private interface IConfig {
    var port: Int
}