package com.qtekfun.ultimatefiles.data.service

import android.net.wifi.WifiManager

/**
 * Wi-Fi lock mode for a transfer. `FULL_HIGH_PERF` is deprecated and, from Android 10, the system maps it to
 * `FULL_LOW_LATENCY` anyway; asking for that one directly is the same lock without the deprecated constant.
 */
@Suppress("DEPRECATION") // FULL_HIGH_PERF is still the only choice before Android 10
internal fun transferWifiLockMode(sdkInt: Int): Int =
    if (sdkInt >= 29) WifiManager.WIFI_MODE_FULL_LOW_LATENCY else WifiManager.WIFI_MODE_FULL_HIGH_PERF
