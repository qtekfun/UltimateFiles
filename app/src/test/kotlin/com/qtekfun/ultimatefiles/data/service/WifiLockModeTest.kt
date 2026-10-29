package com.qtekfun.ultimatefiles.data.service

import android.net.wifi.WifiManager
import org.junit.Assert.assertEquals
import org.junit.Test

class WifiLockModeTest {
    @Test
    fun `uses the low latency mode from Android 10`() {
        listOf(29, 31, 35, 37).forEach { sdk ->
            assertEquals("sdk $sdk", WifiManager.WIFI_MODE_FULL_LOW_LATENCY, transferWifiLockMode(sdk))
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `keeps the high performance mode before Android 10`() {
        listOf(26, 28).forEach { sdk ->
            assertEquals("sdk $sdk", WifiManager.WIFI_MODE_FULL_HIGH_PERF, transferWifiLockMode(sdk))
        }
    }
}
