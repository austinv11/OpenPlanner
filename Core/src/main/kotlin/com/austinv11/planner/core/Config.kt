package com.austinv11.planner.core

import com.austinv11.planner.core.util.serialize
import com.google.gson.GsonBuilder
import io.jsonwebtoken.impl.crypto.MacProvider
import java.io.File
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.memberProperties

object Config : IConfig { //TODO: Secure the config file

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
    override var enforce_account_verification: Boolean by ConfigDelegate<Boolean>()
    override var password_requirements_regex: String? by ConfigDelegate<String?>()
    override var no_auth: Boolean by ConfigDelegate<Boolean>()
    override var signature_key: ByteArray by ConfigDelegate<ByteArray>()
    override var token_expiration: Long by ConfigDelegate<Long>()

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
    
    private data class BackingConfigObject(override var port: Int = 3000,
                                           override var enforce_account_verification: Boolean = true,
                                           override var password_requirements_regex: String? = null,
                                           override var no_auth: Boolean = false,
                                           override var signature_key: ByteArray = MacProvider.generateKey().serialize(), 
                                           override var token_expiration: Long = -1) : IConfig
}

private interface IConfig {
    /**
     * This represents the port this server runs on.
     */
    var port: Int
    /**
     * This represents whether the auth-flow will enforce account verification as a login requirement.
     */
    var enforce_account_verification: Boolean
    /**
     * This represents the regex used to determine if a password is valid for account creation, when null there is no
     * password requirement check.
     */
    var password_requirements_regex: String?
    /**
     * This represents if the auth process is bypassed altogether on login (i.e. passwords are not checked to log into
     * a user's account).
     */
    var no_auth: Boolean
    /**
     * This is a key used to sign generated auth tokens. This is generated via a securely random generator and encoded 
     * with SHA-512 by default on first init.
     */
    var signature_key: ByteArray
    /**
     * This is the time in hours before a token expires, -1 denotes that a token will never expire (unless the server
     * session is reset).
     */
    var token_expiration: Long
}
