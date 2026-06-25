package com.movtery.zalithlauncher.game.control.legacy

import java.io.File

data class LegacyControlData(
    val file: File,
    val info: LegacyControlInfo,
    val buttonCount: Int,
    val drawerCount: Int,
    val joystickCount: Int,
    val formatVersion: Int
)
