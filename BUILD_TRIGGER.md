# Build Trigger

This file triggers the final GitHub Actions APK build.

## Background Task Minimize / Click-to-Restore Fix

Fixed three issues with the background download/install task system introduced
in the previous session's minimize feature.

### Issue 1 — Background task progress freezes after minimize

**Root cause:** `createBackgroundTask()` in both `GameInstaller` and
`ModPackInstaller` used `tasksFlow.collect {}` to mirror sub-task progress.
`TaskFlowExecutor.tasksFlow` only re-emits when the *phase list* changes (a new
`TitledTask` is added), not when individual `Task.currentProgress` (a Compose
`mutableStateOf`) mutates within an existing phase. As a result, the proxy task
stopped updating after the first phase.

**Fix:** Replaced the `collect {}` with a `while (true) { delay(150); read
tasksFlow.value }` polling loop in both installers. The loop correctly reads the
current Compose state on every tick and mirrors it to the proxy task.

### Issue 2 — Clicking a minimized task does nothing

**Root cause:** `TaskMenu`/`TaskItem` in `MainScreen.kt` had no click handler and
no way to link a background `Task.id` back to the live installer.

**Fix:**
- Added `InstallerRestoreRegistry` singleton (new
  `coroutine/InstallerRestoreRegistry.kt`) mapping background task IDs to
  `RestorableInstaller(title, tasksFlow, onCancel)` entries.
- On minimize (`DownloadGameScreen`, `DownloadModPackScreen`): register the entry,
  submit the proxy task with an `onEnded` callback that unregisters on completion.
- `TaskMenu` in `MainScreen.kt`: added `restoredEntry` state + a
  `TitleTaskFlowDialog` overlay that appears when a registered task is tapped.
- `TaskItem` gains an `onTaskClick: (() -> Unit)?` parameter; tapping it when the
  task has a registry entry sets `restoredEntry`, showing the restore dialog.

### Issue 3 — Modpack/version installs randomly stall

Identical root cause to Issue 1 (frozen progress reads like a stall). The polling
fix in `ModPackInstaller.createBackgroundTask()` resolves this.

### Files changed

| File | Change |
|------|--------|
| `coroutine/InstallerRestoreRegistry.kt` | **New** — singleton restore registry |
| `game/download/game/GameInstaller.kt` | Polling loop replaces `collect {}` in `createBackgroundTask` |
| `game/download/modpack/install/ModPackInstaller.kt` | Same polling fix |
| `ui/screens/content/download/DownloadGameScreen.kt` | Register + unregister on minimize |
| `ui/screens/content/download/DownloadModPackScreen.kt` | Register + unregister on minimize |
| `ui/screens/main/MainScreen.kt` | Restore dialog, `onTaskClick` in TaskItem |

Build date: 2026-07-06
