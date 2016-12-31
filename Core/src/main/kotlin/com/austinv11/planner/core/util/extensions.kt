package com.austinv11.planner.core.util

import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * This serializes an object into a byte array.
 * @return The serialized data.
 */
fun Serializable.serialize(): ByteArray {
    val byteStream = ByteArrayOutputStream()
    val objectStream = ObjectOutputStream(byteStream)
    objectStream.writeObject(this)
    objectStream.close()
    return byteStream.toByteArray()
}

/**
 * This deserializes an object from a byte array.
 * @return The deserialized object.
 */
inline fun <reified T: Serializable> ByteArray.deserialize(): T {
    val objectStream = ObjectInputStream(this.inputStream())
    val output = objectStream.readObject() as T
    objectStream.close()
    return output
}
