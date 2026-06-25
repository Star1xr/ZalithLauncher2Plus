package com.movtery.zalithlauncher.game.control.legacy

import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.InputStream

private const val TAG = "LegacyControlManager"

object LegacyControlManager {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _dataList = MutableStateFlow<List<LegacyControlData>>(emptyList())
    val dataList = _dataList.asStateFlow()

    private var currentJob: Job? = null

    private val _selectedLayout = MutableStateFlow<LegacyControlData?>(null)
    val selectedLayout = _selectedLayout.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private fun newFile(): File {
        val ts = System.currentTimeMillis()
        val rnd = (1000..9999).random()
        return File(PathManager.DIR_LEGACY_CONTROL_LAYOUTS, "${ts}_${rnd}.json")
    }

    fun refresh() {
        currentJob?.cancel()
        currentJob = scope.launch(Dispatchers.IO) {
            _isRefreshing.update { true }
            _dataList.update { emptyList() }

            val files = (PathManager.DIR_LEGACY_CONTROL_LAYOUTS.listFiles() ?: emptyArray())
                .filter { it.isFile && it.exists() && it.extension.equals("json", ignoreCase = true) }

            val loaded = files.mapNotNull { file ->
                try {
                    val jsonString = file.readText()
                    if (!LegacyControlParser.isLegacyFormat(jsonString)) return@mapNotNull null
                    val controls = LegacyControlParser.parse(jsonString) ?: return@mapNotNull null
                    LegacyControlData(
                        file = file,
                        info = controls.info,
                        buttonCount = controls.buttonCount,
                        drawerCount = controls.drawerCount,
                        joystickCount = controls.joystickCount,
                        formatVersion = controls.formatVersion
                    )
                } catch (e: Exception) {
                    Logger.warning(TAG, "Failed to load legacy layout: ${file.name}", e)
                    null
                }
            }.sortedBy { it.info.name.ifEmpty { it.file.name } }

            _dataList.update { loaded }
            checkSettings()
            _isRefreshing.update { false }
        }
    }

    private fun checkSettings() {
        val setting = AllSettings.legacyControlLayout.getValue()
        val layout = _dataList.value.find { it.file.name == setting }
            ?: _dataList.value.firstOrNull()?.also { AllSettings.legacyControlLayout.save(it.file.name) }
        if (layout == null) AllSettings.legacyControlLayout.reset()
        _selectedLayout.update { layout }
    }

    fun selectControl(data: LegacyControlData) {
        if (!data.file.exists()) return
        AllSettings.legacyControlLayout.save(data.file.name)
        _selectedLayout.update { data }
    }

    fun deleteControl(data: LegacyControlData) {
        scope.launch(Dispatchers.IO) {
            if (!data.file.exists()) return@launch
            FileUtils.deleteQuietly(data.file)
            refresh()
        }
    }

    fun saveInfo(data: LegacyControlData, newInfo: LegacyControlInfo) {
        scope.launch(Dispatchers.IO) {
            try {
                val jsonString = data.file.readText()
                val updated = LegacyControlParser.updateInfo(jsonString, newInfo)
                data.file.writeText(updated)
                refresh()
            } catch (e: Exception) {
                Logger.warning(TAG, "Failed to save legacy control info: ${data.file.name}", e)
            }
        }
    }

    suspend fun importControl(
        inputStream: InputStream,
        onNotLegacy: () -> Unit,
        onError: (Exception) -> Unit,
        onFinished: () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val destFile = newFile()
        try {
            val jsonString = inputStream.use { it.bufferedReader().readText() }
            if (!LegacyControlParser.isLegacyFormat(jsonString)) {
                onNotLegacy()
                return@withContext
            }
            if (LegacyControlParser.parse(jsonString) == null) {
                onError(Exception("Invalid legacy control format"))
                return@withContext
            }
            destFile.writeText(jsonString)
            refresh()
            onFinished()
        } catch (e: Exception) {
            FileUtils.deleteQuietly(destFile)
            onError(e)
        }
    }
}
