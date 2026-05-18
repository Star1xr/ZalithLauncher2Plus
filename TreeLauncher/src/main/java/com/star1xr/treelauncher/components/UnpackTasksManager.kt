package com.star1xr.treelauncher.components

import android.content.Context
import com.star1xr.treelauncher.R
import com.star1xr.treelauncher.components.jre.Jre
import com.star1xr.treelauncher.components.jre.UnpackJnaTask
import com.star1xr.treelauncher.components.jre.UnpackJreTask
import com.star1xr.treelauncher.setting.AllSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

object UnpackTasksManager {
    private val _unpackItems = mutableListOf<InstallableItem>()
    val unpackItems: List<InstallableItem> = _unpackItems
    private val _finishedTaskCount = AtomicInteger(0)
    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    fun initUnpackItems(context: Context) {
        if (_unpackItems.isNotEmpty()) return
        Components.entries.forEach { component ->
            val task = UnpackComponentsTask(context, component)
            if (!task.isCheckFailed()) {
                _unpackItems.add(InstallableItem(component.displayName, context.getString(component.summary), task))
            }
        }
        Jre.entries.forEach { jre ->
            val task = UnpackJreTask(context, jre)
            if (!task.isCheckFailed()) {
                _unpackItems.add(InstallableItem(jre.jreName, context.getString(jre.summary), task))
            }
        }
        val jnaTask = UnpackJnaTask(context)
        if (!jnaTask.isCheckFailed()) {
            _unpackItems.add(InstallableItem("JNA", context.getString(R.string.unpack_screen_jna), jnaTask))
        }
        _unpackItems.sort()
        _unpackItems.forEach { item ->
            val state = item.task.checkState()
            item.updateState(state)
            if (state == InstallableItem.State.FINISHED) _finishedTaskCount.incrementAndGet()
        }
        updateProgress()
    }

    private fun updateProgress() {
        if (_unpackItems.isEmpty()) { _progress.value = 1f; return }
        _progress.value = _finishedTaskCount.get().toFloat() / _unpackItems.size
    }

    fun areAllTasksFinished() = _finishedTaskCount.get() >= _unpackItems.size

    fun startAllTask(scope: CoroutineScope, onFinished: () -> Unit = {}) {
        if (areAllTasksFinished()) { onFinished(); return }
        scope.launch {
            _unpackItems.filter { it.state.value == InstallableItem.State.NOT_STARTED || it.state.value == InstallableItem.State.PENDING }
                .map { item ->
                    launch(Dispatchers.IO) {
                        item.updateState(InstallableItem.State.RUNNING)
                        runCatching { item.task.run() }
                        _finishedTaskCount.incrementAndGet()
                        item.updateState(InstallableItem.State.FINISHED)
                        updateProgress()
                    }
                }.joinAll()
            AllSettings.javaRuntime.apply { if (getValue().isEmpty()) save(Jre.JRE_8.jreName) }
            onFinished()
        }
    }
}
