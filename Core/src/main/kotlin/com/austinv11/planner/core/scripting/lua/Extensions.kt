package com.austinv11.planner.core.scripting.lua

import org.luaj.vm2.*
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.CoerceLuaToJava
import java.lang.reflect.Method
import kotlin.reflect.functions
import kotlin.reflect.jvm.javaMethod

/**
 * This is used to mark an api for usage in lua scripting.
 */
abstract class ILuaAPI {

    /**
     * This is the name of the object which will be used to call its methods.
     */
    abstract val name: String

    /**
     * This converts the [ILuaAPI] object into a [TwoArgFunction] object which is used to represent a global api in lua.
     * @return The [TwoArgFunction] object representing the api.
     */
    fun toLuaLib(): TwoArgFunction {
        return object: TwoArgFunction() {
            
            override fun call(modname: LuaValue?, env: LuaValue?): LuaValue {
                val table = LuaTable()
                (this@ILuaAPI.javaClass.fields+this@ILuaAPI.javaClass.declaredFields).map { 
                    it.isAccessible = true
                    return@map it
                }.filter{ 
                    it.isAnnotationPresent(Constant::class.java) 
                }.forEach { 
                    table.set(it.name, coerceWithTables(it.get(this@ILuaAPI))) 
                }
                compileFunctions().forEach { k, v -> table.set(k, v) }
                env?.set(this@ILuaAPI.name, table)
                env?.get("package")?.get("loaded")?.set(this@ILuaAPI.name, table)
                return table
            }
        }
    }

    /**
     * This converts the [ILuaAPI] object into a [LuaValue] which can be returned by lua apis.
     * @return The [LuaValue] which can be passed to a lua script.
     */
    fun toLuaObj(): LuaValue {
        return object: LuaValue() {
            val table = LuaTable() 
            
            init {
                (this@ILuaAPI.javaClass.fields+this@ILuaAPI.javaClass.declaredFields).map {
                    it.isAccessible = true
                    return@map it
                }.filter{
                    it.isAnnotationPresent(Constant::class.java)
                }.forEach {
                    table.set(it.name, coerceWithTables(it.get(this@ILuaAPI)))
                }

                compileFunctions().forEach { k, v -> table.set(k, v) }
            }
            
            override fun typename() = "userdata"
            override fun type() = LuaValue.TUSERDATA
            override fun tojstring() = this@ILuaAPI.name
            override fun get(key: LuaValue): LuaValue = table.get(key)
        }
    }
    
    private fun compileFunctions(): Map<String, LuaFunction> {
        val map = mutableMapOf<String, MutableList<Method>>()
        
        this::class.functions.filter { 
            it.annotations.find { Function::class.java.isAssignableFrom(it.javaClass) } != null 
        }.forEach { 
            map.putIfAbsent(it.name, mutableListOf())
            
            map[it.name]!!.add(it.javaMethod!!)
        }
        
        return map.mapValues { 
            return@mapValues VarargsFunctionWrapper(this@ILuaAPI, it.value.toTypedArray())
        }
    }
    
    private class VarargsFunctionWrapper(val objectReference: Any?, functions: Array<Method>): VarArgFunction() {
        
        val nargSortedFunctions = mutableMapOf<Int, MutableList<Method>>()
        val varargAcceptingFunctions = mutableListOf<Method>()
        
        init {
            functions.forEach {
                if (it.parameterTypes.contains(Varargs::class.java)) {
                    varargAcceptingFunctions.add(it)
                } else {
                    nargSortedFunctions.putIfAbsent(it.parameterCount, mutableListOf()) //Ensure lists are already initialized

                    nargSortedFunctions[it.parameterCount]!!.add(it)
                }
            }
        }
        
        override fun invoke(args: Varargs?): Varargs {
            val key = args?.narg() ?: 0
            if (nargSortedFunctions.containsKey(key) || varargAcceptingFunctions.size > 0) {
                val functions = if (nargSortedFunctions.containsKey(key)) nargSortedFunctions[key]!!.toMutableList() else mutableListOf()
                functions.addAll(varargAcceptingFunctions)
                
                if (key == 0) { //Zero args, no point in checking function args
                    return coerceWithTables(functions.first()!!.invoke(objectReference))
                }
                
                args!!
                
                functions.forEach { 
                    val coercedValues = mutableListOf<Any?>()
                    var success: Boolean = false
                    
                    it.parameterTypes.forEachIndexed { i, clazz -> 
                        val sanitizedClass = sanitizeClass(clazz)
                        val coerced: Any
                        var isUsingLuaValue = false
                        if (LuaValue::class.java.isAssignableFrom(clazz)) { //Special handling if the api can accept lua values
                            isUsingLuaValue = true
                            coerced = args.arg(i+1)
                        } else if (Varargs::class.java.isAssignableFrom(clazz)) { //Special handling if the api can accept varargs
                            isUsingLuaValue = true
                            val varargArgs = mutableListOf<LuaValue>()
                            for (j in (i+1)..(args.narg()+1)) {
                                varargArgs.add(args.arg(j))
                            }
                            coerced = LuaValue.varargsOf(varargArgs.toTypedArray())
                        } else {
                            coerced = CoerceLuaToJava.coerce(args.arg(i+1), sanitizedClass)
                        }
                        if (coerced::class.java != sanitizedClass && (!isUsingLuaValue && LuaValue::class.java.isAssignableFrom(coerced::class.java))) { //Coercion failed and falled back to LuaValue
                            success = false
                            return@forEachIndexed
                        } else {
                            success = true
                            coercedValues.add(coerced)
                        }
                    }
                    
                    if (success) { //All coercions were successful!
                        return@invoke coerceWithTables(it.invoke(objectReference, *coercedValues.toTypedArray()))
                    }
                }
            }
            
            throw LuaError("No suitable function found!")
        }
    }
}

/**
 * This marks a function to be exposed to lua.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Function

/**
 * This marks a field to be exposed to lua.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Constant

private fun sanitizeClass(`class`: Class<*>): Class<*> { //Ensures that only vanilla java classes are used for primitives
    if (`class`.isArray) {
        if (IntArray::class.java.isAssignableFrom(`class`)) {
            return Array<java.lang.Integer>::class.java
        } else if (ByteArray::class.java.isAssignableFrom(`class`)) {
            return Array<java.lang.Byte>::class.java
        } else if (ShortArray::class.java.isAssignableFrom(`class`)) {
            return Array<java.lang.Short>::class.java
        } else if (DoubleArray::class.java.isAssignableFrom(`class`)) {
            return Array<java.lang.Double>::class.java
        } else if (LongArray::class.java.isAssignableFrom(`class`)) {
            return Array<java.lang.Long>::class.java
        } else if (FloatArray::class.java.isAssignableFrom(`class`)) {
            return Array<java.lang.Float>::class.java
        } else if (BooleanArray::class.java.isAssignableFrom(`class`)) {
            return Array<java.lang.Boolean>::class.java
        } else if (CharArray::class.java.isAssignableFrom(`class`)) {
            return Array<java.lang.Character>::class.java
        } else if (Array<String>::class.java.isAssignableFrom(`class`)) {
            return Array<java.lang.String>::class.java
        } else if (Array<Any>::class.java.isAssignableFrom(`class`)) {
            return Array<java.lang.Object>::class.java
        } else {
            return `class`
        }
    } else if (Int::class.java.isAssignableFrom(`class`)) {
        return java.lang.Integer::class.java
    } else if (Byte::class.java.isAssignableFrom(`class`)) {
        return java.lang.Byte::class.java
    } else if (Short::class.java.isAssignableFrom(`class`)) {
        return java.lang.Short::class.java
    } else if (Double::class.java.isAssignableFrom(`class`)) {
        return java.lang.Double::class.java
    } else if (Long::class.java.isAssignableFrom(`class`)) {
        return java.lang.Long::class.java
    }else if (Float::class.java.isAssignableFrom(`class`)) {
        return java.lang.Float::class.java
    } else if (Boolean::class.java.isAssignableFrom(`class`)) {
        return java.lang.Boolean::class.java
    } else if (Unit::class.java.isAssignableFrom(`class`)) {
        return java.lang.Void::class.java
    } else if (Char::class.java.isAssignableFrom(`class`)) {
        return java.lang.Character::class.java
    } else if (String::class.java.isAssignableFrom(`class`)) {
        return java.lang.String::class.java
    } else if (Any::class.java.isAssignableFrom(`class`)) {
        return java.lang.Object::class.java
    } else {
        return `class`
    }
}

/**
 * Adds better support for map and array coercion.
 */
fun coerceWithTables(obj: Any?): LuaValue {
    if (obj != null) {
        if (obj is LuaValue) {
            return obj
        } else if (obj is ILuaAPI) {
            return obj.toLuaObj()
        } else if (obj.javaClass.isArray) {
            val table = LuaTable()
            (obj as Array<*>).forEachIndexed { i, any -> 
                table.set(i+1, coerceWithTables(any))
            }
            return table
        } else if (Collection::class.java.isAssignableFrom(obj.javaClass)) {
            val table = LuaTable()
            (obj as Collection<*>).forEachIndexed { i, any ->
                table.set(i+1, coerceWithTables(any))
            }
            return table
        } else if (Map::class.java.isAssignableFrom(obj.javaClass)) {
            val table = LuaTable()
            (obj as Map<*, *>).forEach { k, v -> 
                table.set(coerceWithTables(k), coerceWithTables(v))
            }
            return table
        }
    }
    
    return CoerceJavaToLua.coerce(obj)
}
