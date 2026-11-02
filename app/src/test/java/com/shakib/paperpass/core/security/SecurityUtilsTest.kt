package com.shakib.paperpass.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun `hashPassword produces deterministic SHA-256 output`() {
        val raw = "secretPassword123"
        val hash1 = SecurityUtils.hashPassword(raw)
        val hash2 = SecurityUtils.hashPassword(raw)

        assertEquals("Hashes should be identical for identical inputs", hash1, hash2)
        assertEquals(64, hash1.length) // 256 bits = 64 hex characters
    }

    @Test
    fun `different passwords generate different hashes`() {
        val hash1 = SecurityUtils.hashPassword("PasswordA")
        val hash2 = SecurityUtils.hashPassword("PasswordB")

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `verifyPassword validates matching credentials`() {
        val raw = "mypassword"
        val hash = SecurityUtils.hashPassword(raw)

        assertTrue(SecurityUtils.verifyPassword("mypassword", hash))
        assertFalse(SecurityUtils.verifyPassword("wrongpassword", hash))
    }

    @Test
    fun `isPinValidFormat enforces 4 numeric digits`() {
        assertTrue(SecurityUtils.isPinValidFormat("1234"))
        assertTrue(SecurityUtils.isPinValidFormat("0000"))
        assertFalse(SecurityUtils.isPinValidFormat("123"))
        assertFalse(SecurityUtils.isPinValidFormat("12345"))
        assertFalse(SecurityUtils.isPinValidFormat("123a"))
        assertFalse(SecurityUtils.isPinValidFormat(""))
    }
}

