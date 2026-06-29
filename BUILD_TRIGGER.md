# Build Trigger

This file triggers the final GitHub Actions APK build.

## Upstream Sync: post-v2.4.9_hotfix1

Synced with Star1xr/ZalithLauncher2Plus upstream (1b8c96c5).

### New upstream changes integrated:
- CurseForge API key support during file downloads
- JVM argument parsing fix (concatenation bug)
- In-game clipboard null safety fix  
- Dependency library updates
- Scrollbar added to version list screen
- 14 new HTTP error string resources (400/401/403/404/408/409/410/429/500/502/503/504)

### Merge conflicts resolved (7 files):
- README.md — kept Zeryth branding description
- gradle.properties — kept our indented format
- strings.xml — merged all upstream HTTP error strings
- VersionsManageScreen.kt — added scrollbar + kept onLaunchClick
- VersionsManageElements.kt — kept our imports, modifier-first Image
- ControlManageScreen.kt — merged imports, removed duplicates
- ControlSettingsScreen.kt — kept verticalScrollWithBar import

### Preserved Zeryth features:
- Bottom Navigation (NOT replaced by upstream sidebar)
- Zeryth Launcher branding
- Legacy Controls (ZL1 Backport)
- Custom Material 3 redesign
- Quick Access Panel
- Statistics page
- Version cards/grid/list
- All custom UI animations

Build date: 2026-06-29
