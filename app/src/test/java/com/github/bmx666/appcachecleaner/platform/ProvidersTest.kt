package com.github.bmx666.appcachecleaner.platform

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProvidersTest {

    @Test
    fun `sdk helpers compare against the provided level`() {
        val o = object : SdkProvider { override val sdkInt = Build.VERSION_CODES.O }
        assertTrue(o.atLeastO())
        assertFalse(o.atLeastU())

        val m = object : SdkProvider { override val sdkInt = Build.VERSION_CODES.M }
        assertFalse(m.atLeastO())
        assertFalse(m.atLeastN())
    }

    @Test
    fun `default dispatcher provider exposes the standard dispatchers`() {
        val p = DefaultDispatcherProvider()
        assertEquals(kotlinx.coroutines.Dispatchers.IO, p.io)
        assertEquals(kotlinx.coroutines.Dispatchers.Main, p.main)
        assertEquals(kotlinx.coroutines.Dispatchers.Default, p.default)
    }
}
