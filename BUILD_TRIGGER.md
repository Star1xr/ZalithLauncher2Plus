# Build Trigger

This file triggers the final GitHub Actions APK build.

## Fix: missing InstallerRestoreRegistry import (build retry)

Added missing `import com.movtery.zalithlauncher.coroutine.InstallerRestoreRegistry`
to `DownloadGameScreen.kt` and `DownloadModPackScreen.kt`. The class exists and the
registry usage was correct; the import statement was not retained when the onMinimize
handler was patched in the previous commit.

## Background Task Minimize / Click-to-Restore Fix (summary)

Three issues fixed across 6 files:

| Fix | Files |
|-----|-------|
| Progress polling: replace `collect {}` with `while(true) { delay(150); tasksFlow.value }` | `GameInstaller.kt`, `ModPackInstaller.kt` |
| New `InstallerRestoreRegistry` singleton | `InstallerRestoreRegistry.kt` (new) |
| Register on minimize, unregister on task end | `DownloadGameScreen.kt`, `DownloadModPackScreen.kt` |
| Click-to-restore dialog in `TaskMenu`, `onTaskClick` in `TaskItem` | `MainScreen.kt` |

Build date: 2026-07-06
