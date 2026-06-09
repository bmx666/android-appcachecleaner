package com.github.bmx666.appcachecleaner.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class ByteFormatterTest {

    private val us = Locale.US

    @Test
    fun `format picks unit by 900 threshold`() {
        assertEquals("0 KB", ByteFormatter.format(0, us))
        assertEquals("1 KB", ByteFormatter.format(1024, us))
        assertEquals("1.5 KB", ByteFormatter.format(1536, us))
        assertEquals("900 KB", ByteFormatter.format(900L * 1024, us))
        assertEquals("1 MB", ByteFormatter.format(1024L * 1024, us))
        assertEquals("1 GB", ByteFormatter.format(1024L * 1024 * 1024, us))
    }

    @Test
    fun `format rounds half up to two fraction digits`() {
        // 500 / 1024 = 0.48828... -> 0.49
        assertEquals("0.49 KB", ByteFormatter.format(500, us))
    }

    @Test
    fun `parse accepts magnitude with optional unit and spaces`() {
        assertEquals(1024L, ByteFormatter.parse("1KB"))
        assertEquals(1024L, ByteFormatter.parse("1 KB"))
        assertEquals(1024L, ByteFormatter.parse("1024"))
        assertEquals(1536L, ByteFormatter.parse("1.5KB"))
        assertEquals(10L * 1024 * 1024, ByteFormatter.parse("10 MB"))
        assertEquals(5L * 1024 * 1024 * 1024, ByteFormatter.parse("5GB"))
        assertEquals(0L, ByteFormatter.parse("0"))
    }

    @Test
    fun `parse rejects malformed and negative input`() {
        assertNull(ByteFormatter.parse(""))
        assertNull(ByteFormatter.parse("   "))
        assertNull(ByteFormatter.parse("abc"))
        assertNull(ByteFormatter.parse("12XB"))
        assertNull(ByteFormatter.parse("-1"))
    }
}
