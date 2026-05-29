package com.movtery.zalithlauncher.utils.fsr

import com.movtery.zalithlauncher.bridge.ZLBridge
import com.movtery.zalithlauncher.setting.AllSettings

object FSRUtils {
    private var loaded = false

    fun load() {
        if (loaded) return
        if (!AllSettings.fsrEnabled.getValue()) return

        AllSettings.resolutionRatio.updateState(100)

        if (ZLBridge.dlopen("libgl_fsr.so")) {
            loaded = true
            ZLBridge.fsrInit(AllSettings.fsrQuality.getValue())
        }
    }

    fun destroy() {
        if (!loaded) return
        loaded = false
    }
}