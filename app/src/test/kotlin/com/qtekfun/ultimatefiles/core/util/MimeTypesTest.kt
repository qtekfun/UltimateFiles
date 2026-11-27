package com.qtekfun.ultimatefiles.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MimeTypesTest {
    @Test
    fun `an apk is recognised by its type or its extension`() {
        assertTrue(MimeTypes.isApk("download", MimeTypes.APK))
        assertTrue(MimeTypes.isApk("app.apk", MimeTypes.OCTET_STREAM))
        assertTrue(MimeTypes.isApk("APP.APK", null))
    }

    @Test
    fun `other files are not apks`() {
        assertFalse(MimeTypes.isApk("app.apk.txt", "text/plain"))
        assertFalse(MimeTypes.isApk("apk", MimeTypes.OCTET_STREAM))
        assertFalse(MimeTypes.isApk("data.bin", null))
    }
}
