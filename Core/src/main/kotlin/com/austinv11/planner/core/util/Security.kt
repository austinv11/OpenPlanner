package com.austinv11.planner.core.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.security.Key
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

/**
 * This object contains various security-related utilities.
 */
object Security {
    
    val SHA1 = SecureRandom.getInstance("SHA1PRNG")
    val SHA256 = MessageDigest.getInstance("SHA-256")
    val SHA512 = MessageDigest.getInstance("SHA-512")
    const val SALT_LENGTH = 64

    /**
     * This generates a random salt via [generateSalt] and then hashes the provided string via [hash].
     * @return A pair, firstValue=salt, secondValue=hash.
     */
    fun hash(password: String): Pair<ByteArray, ByteArray> {
        val salt = generateSalt()
        return salt to hash(password, salt)
    }

    /**
     * This hashes a password with a given salt by first hashing the password with the SHA-256 algorithm, it then 
     * combines the initial hash with the salt and hashes that product with the SHA-512 algorithm.
     * @return The salted hash.
     */
    fun hash(password: String, salt: ByteArray): ByteArray {
        val initialHash = SHA256.digest(password.toByteArray())
        val finalHash = SHA512.digest(initialHash + salt)
        return finalHash
    }

    /**
     * This performs a hash operation on the provided string and salt in order to compare it to the supposed value of 
     * the salted hash.
     * @return True if the hashes match, false if otherwise.
     */
    fun verify(hash: ByteArray, password: String, salt: ByteArray): Boolean {
        return Arrays.equals(hash(password, salt), hash)
    }

    /**
     * This generates a securely random salt that is [SALT_LENGTH] bytes long using the SHA-1 algorithm.
     * @return The random salt.
     */
    fun generateSalt(): ByteArray {
        val array = ByteArray(SALT_LENGTH)
        SHA1.nextBytes(array)
        return array
    }

    /**
     * This generates a SHA-512 signed JWS string.
     * @param key The key to sign with (must be SHA-512 encoded).
     * @param claims The claims designated as the payload.
     * @return The JWS string.
     */
    fun generateJWS(key: Key, claims: Map<String, Any?>): String {
        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS512, key)
                .setClaims(claims)
                .compact()
    }

    /**
     * This attempts to parse a signed JWS string.
     * @param key The key used to sign the string.
     * @param jws The JWS string to parse.
     * @return The claims (body) of the jws string.
     */
    fun parseJWS(key: Key, jws: String): Map<String, Any?> {
        return Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(jws)
                .body
    }
}
