package com.movtery.zalithlauncher.utils.fsr

import com.movtery.zalithlauncher.bridge.ZLBridge
import com.movtery.zalithlauncher.setting.AllSettings

object FSRUtils {
    private var loaded = false

    // Scale factors match fsr_hook.cpp calcRenderResolution:
    // 1=UltraQuality(1.3x), 2=Quality(1.5x), 3=Balanced(1.7x), 4=Performance(2.0x)
    fun qualityToResolutionRatio(preset: Int): Int = when (preset) {
        1 -> 77
        2 -> 67
        3 -> 59
        4 -> 50
        else -> 67
    }

    fun load() {
        if (loaded) return
        if (!AllSettings.fsrEnabled.getValue()) return

        val quality = AllSettings.fsrQuality.getValue()
        AllSettings.resolutionRatio.updateState(qualityToResolutionRatio(quality))

        if (ZLBridge.dlopen("libzl_fsr.so")) {
            loaded = true
            ZLBridge.fsrInit(quality)
        }
    }

    fun updateQuality(quality: Int) {
        AllSettings.resolutionRatio.updateState(qualityToResolutionRatio(quality))
        AllSettings.resolutionRatio.save()
        if (loaded) ZLBridge.fsrSetQuality(quality)
    }

    fun destroy() {
        if (!loaded) return
        AllSettings.resolutionRatio.updateState(100)
        loaded = false
    }
}