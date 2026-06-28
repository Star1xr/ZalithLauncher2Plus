# Build Trigger

  This file was updated to trigger the final GitHub Actions APK build.

  ## Upstream Sync: v2.4.7 → v2.4.9_hotfix1

  Integrated 88 upstream commits from Star1xr/ZalithLauncher2Plus (fix branch):

  ### Bug Fixes
  - CurseForge API key injection via OkHttp interceptor (new CurseForge features work correctly)
  - OkHttp file download client (DOWNLOAD_OKHTTP_CLIENT) - fixes download timeout/hang issues
  - Duration API migration for delay() calls throughout codebase
  - Version enum serialization fix - prevents ProGuard obfuscation breaking version configs
  - Clipboard null-safety fix in CallbackBridge (getClipboardText)
  - Multi-window mode title bar safe area fix
  - Always provide haze source fix

  ### New Features
  - OptiFine separate mod check (OptiFineModReader) - no longer requires remote project lookup
  - Selectable mod updates (choose which mods to update)
  - Mod update filter improvements (exclude unreachable remote projects)
  - Scrollbars on most scrollable components (verticalScrollWithBar)
  - ModLoaderIcon composable refactor in VersionsManageElements
  - CurseForge download files now include API key

  ### Dependency Updates
  - Material3 rolled back to 1.5.0-alpha20 (stability fix)
  - Other dependency updates (libs.versions.toml)

  ### Translations
  - Vietnamese (full update + new terracotta.xml)
  - Turkish, Portuguese, Chinese, and other language updates

  ### Version
  - Bumped to 2.4.9_hotfix1 (version code 200032)

  ### Zeryth Customizations Preserved
  - Zeryth branding (launcher_name, launcher_app_name, URL_PROJECT, URL_RAMI1L)
  - OAuth client ID (00000000402b5328) for Microsoft login
  - Bottom Navigation (LauncherScreen untouched)
  - Dashboard/Stats/Version cards/grid implementation
  - Legacy ZL1 control support in ControlManageScreen
  - LegacyControlEditorActivity + ZL1 stubs in CallbackBridge
  - All Zeryth-specific string resources
  - Microsoft auth improvements
  