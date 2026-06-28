# Legacy Control Editor — Fix Summary

## Changes in this build

### 1. Fix: Legacy editor exit never worked (CustomControlsActivity.java)
- Added `exitEditor()` override that calls `finish()`.
- EditorExitable default is a no-op; without this override confirming any exit or
  save-and-exit dialog did nothing and the user was permanently stuck in the editor.

### 2. Fix: Save button wrote to wrong directory (CustomControlsActivity.java)
- ControlMenu's default `save`/`saveAndExit` actions call `openSaveDialog()` which
  saves to DIR_CTRLMAP_PATH (the Zalith 2 layout folder), not the legacy file path.
- Custom click listeners now call `controlLayout.saveLayout(filePath)` directly using
  the path received via BUNDLE_CONTROL_PATH, then call `LegacyControlManager.refresh()`.

### 3. UI: Legacy editor control menu modernised (view_control_menu.xml)
- All buttons converted from plain Android Buttons to MaterialButton (M3 tonal/outlined/filled styles).
- Section dividers and labels added using theme colours (?attr/colorOutlineVariant, ?attr/colorPrimary).
- Exit button uses ?attr/colorError tint to distinguish it from action buttons.
- SeekBar and Switch preserved to maintain ViewBinding type compatibility with ControlMenu.kt.

### 4. UI: Activity menu toggle upgraded to Material 3 FAB (activity_custom_controls.xml)
- ImageButton replaced with FloatingActionButton (mini size).
- FAB uses theme-appropriate tint (?attr/colorOnPrimaryContainer / colorPrimaryContainer).

### 5. Fix: LegacyControlConverter coordinate accuracy (LegacyControlConverter.kt)
- ${preferred_scale} now correctly resolved to 100.0 (was left unresolved → parser 0.5 fallback).
- px(value) function calls now correctly evaluated as value * dpFrac in normalised space.
- ${right}/${bottom} edge anchors expanded before evaluation.
- ${margin} and ${ratio} resolved.
- Remaining unresolved variable tokens stripped to 0.0 via manual scan (avoids Kotlin regex
  dollar-sign interpolation issues).
- All helpers (replacePxCalls, stripUnresolved) use character scanning — no regex — ensuring
  safe handling of the $ character in both JS template strings and Kotlin string literals.
