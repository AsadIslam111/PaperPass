package com.shakib.paperpass.core.security

import java.security.MessageDigest

object SecurityUtils {

    /**
     * Compute SHA-256 cryptographic hash of a raw string with salt.
     */
    fun hashPassword(password: String, salt: String = "PaperPassSecureSalt_2026"): String {
        val input = "$salt:$password"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verify a plaintext password against a stored SHA-256 hash.
     */
    fun verifyPassword(password: String, storedHash: String, salt: String = "PaperPassSecureSalt_2026"): Boolean {
        return hashPassword(password, salt) == storedHash
    }

    /**
     * Verify 4-digit numeric security PIN.
     */
    fun verifyPin(enteredPin: String, storedPin: String): Boolean {
        return enteredPin.trim() == storedPin.trim()
    }

    /**
     * Validate PIN formatting: exactly 4 digits.
     */
    fun isPinValidFormat(pin: String): Boolean {
        return pin.length == 4 && pin.all { it.isDigit() }
    }
}

