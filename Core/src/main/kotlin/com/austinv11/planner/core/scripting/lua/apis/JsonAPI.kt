package com.austinv11.planner.core.scripting.lua.apis

import com.austinv11.planner.core.scripting.lua.Function
import com.austinv11.planner.core.scripting.lua.ILuaAPI
import com.google.gson.*
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

/**
 * This represents a json api which could be accessed by lua scripts.
 */
object JsonAPI: ILuaAPI() {
    override val name: String = "json"
    private val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

    /**
     * This will take a table and convert it into a json compatible string.
     * @param table The table to convert into a json string.
     * @return The json string.
     */
    @Function
    fun toJsonString(table: LuaTable): String {
        return gson.toJson(convertToJsonElement(table))
    }
    
    private fun convertToJsonElement(value: LuaValue): JsonElement {
        if (value.istable()) {
            val table = value.checktable()
            if (isArray(table)) {
                val array = JsonArray()
                table.keys().map { table.get(it) }.map { convertToJsonElement(it) }.forEach(array::add)
                return array
            } else {
                val obj = JsonObject()
                table.keys().forEach { 
                    obj.add(it.tojstring(), convertToJsonElement(table.get(it)))
                }
                return obj
            }
        } else {
            if (value.isnumber()) {
                if (value.isint()) {
                    return JsonPrimitive(value.toint())
                } else {
                    return JsonPrimitive(value.tolong())
                }
            } else if (value.isboolean()) {
                return JsonPrimitive(value.toboolean())
            } else if (value.isstring()) {
                val string = value.tojstring()
                if (string.length == 1) {
                    return JsonPrimitive(string.first())
                } else {
                    return JsonPrimitive(string)
                }
            } else if (value.isnil()) {
                return JsonNull.INSTANCE
            }
        }
        
        return JsonNull.INSTANCE
    }
    
    private fun isArray(table: LuaTable): Boolean {
        var isArray = true
        table.keys().forEach {
            if (!it.isnumber()) {
                isArray = false
                return@forEach
            }
        }
        return isArray
    }
    
    @Function
    fun parseJsonString(string: String): LuaTable {
        return convertToLuaValue(JsonParser().parse(string)) as LuaTable
    }
    
    private fun convertToLuaValue(value: JsonElement): LuaValue {
        if (value.isJsonArray) {
            val table = LuaTable()
            val array = value.asJsonArray
            for (i in 0..array.size()) {
                table.set(i+1, convertToLuaValue(array.get(i)))
            }
            return table
        } else {
            if (value.isJsonObject) {
                val table = LuaTable()
                val obj = value.asJsonObject
                obj.entrySet().forEach { 
                    val (key, entry) = it
                    table.set(key, convertToLuaValue(entry))
                }
                return table
            } else if (value.isJsonNull) {
                return LuaValue.NIL
            } else if (value.isJsonPrimitive) {
                val primitive = value.asJsonPrimitive
                if (primitive.isBoolean) {
                    return LuaValue.valueOf(primitive.asBoolean)
                } else if (primitive.isNumber) {
                    val num = primitive.asNumber
                    if ((num as Int) == num) { //Is integer
                        return LuaValue.valueOf(num as Int)
                    } else {
                        return LuaValue.valueOf(num as Double)
                    }
                } else if (primitive.isString) {
                    return LuaValue.valueOf(primitive.asString)
                }
            }
        }
        
        return LuaValue.NIL
    }
}
