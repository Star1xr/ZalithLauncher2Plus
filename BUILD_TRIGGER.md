# Build Trigger

This file triggers the final GitHub Actions APK build.

## MobileGlues glFogfv crash fix

Fixed NullPointerException crash in Minecraft 1.16.5 with the MobileGlues renderer.

### Root Cause

MobileGlues (a GLES 3.x-based OpenGL translator) does not export fixed-function
OpenGL 1.x fog functions (`glFogfv`, `glFogf`, `glFogi`, etc.) because GLES has
no fixed-function pipeline. LWJGL3 stores a null pointer for these functions when
initialising `GLCapabilities`, and throws `NullPointerException` via `Checks.check`
the first time Minecraft 1.16.5 calls `glFogfv` during fog rendering.

### Fix

Extended `GLGetProcAddress` in `ZalithLauncher/src/main/jni/ctxbridges/br_loader.c`
to return silent no-op stubs for the GL 1.x fog function family when both
`eglGetProcAddress` and `dlsym` fail to resolve a symbol and `POJAV_RENDERER` is
`mobileglues`. This gives LWJGL3 a valid (non-null) function pointer so the
capabilities check passes, and the no-op body silently ignores the unsupported call
rather than crashing the game.

Stub coverage: `glFogf`, `glFogi`, `glFogfv`, `glFogiv`, `glFogCoordf`,
`glFogCoordd`, `glFogCoordfv`, `glFogCoorddv`, `glFogCoordPointer`.

Other renderers (GL4ES, Zink, VirGL, Freedreno, Panfrost) are unaffected — the
stub path is strictly gated on `POJAV_RENDERER=mobileglues`.

## Quick Actions → Built-in File Manager (Issue #5)

Quick Actions buttons in the Version Overview now open the built-in File Manager
at the appropriate version subdirectory instead of the Android system share menu.

### Changes

- `VersionOverViewScreen.kt` — removed unused `shareFile` import (cleanup).
- `FileManagerScreen.kt` — added `LaunchedEffect(initialPath)` so the screen
  resets its current directory when navigated to with a new `initialPath`. This
  ensures Quick Actions for different version folders each land in the correct place.

The navigation wiring itself (`navigateToFileManager` → `NormalNavKey.FileManager`
→ `FileManagerScreen`) was already in place.

Build date: 2026-07-05
