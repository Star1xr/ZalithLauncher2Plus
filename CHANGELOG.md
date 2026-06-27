# Zeryth Launcher Changelog

  ## [Unreleased]

  ### Features & Fixes

  #### Task 1 – Collapsible Default Control System section
  - `ControlSettingsScreen`: Replaced full-detail card with a compact FilterChip strip
  - Zalith2 / Legacy chips always visible for one-tap switching
  - Swipe **down** on the strip to reveal the full detail panel; swipe **up** on the panel to collapse it
  - Uses `detectVerticalDragGestures` + `AnimatedVisibility(expandVertically/shrinkVertically)`

  #### Tasks 2 & 3 – Fix Legacy Controls in-game + imported control positions
  - `LegacyControlConverter`: full rewrite of `parseExpr` to accept actual button-size fractions
    (`wFrac`, `hFrac`, `dpFrac`) relative to the ZL1 reference screen (1280 × 720 dp)
  - Correctly converts ZL1 **left/top-edge** coordinates → LayerController **CENTER** coordinates
    by adding `halfButtonSize / refAxis` to each axis after evaluation
  - `widthReference` / `heightReference` corrected to `"screen_width"` / `"screen_height"`
  - `textAlignment` serialized as `"Left"` (enum name, no custom `@SerialName`)
  - Includes a minimal recursive-descent `ExprParser` for full arithmetic expression support

  #### Task 4 – ZL1-compatible Control Editor
  - `LegacyControlEditorActivity.evalExpr`: updated to accept `wFrac`, `hFrac`, `dpFrac`
    so ${width}/${height}/${dp} variables evaluate correctly for each button size
  - `parseButtons`: converts ZL1 left/top-edge → CENTER by adding half-size (matching converter)
  - `buildButtonJson`: now saves **left-edge** positions (center − halfSize/refAxis) for full
    ZL1 round-trip compatibility

  #### Task 5 – Replace Quick Access "About" with "Controls" shortcut
  - `LauncherScreen`: Quick Access sidebar now shows **Controls** shortcut instead of About
  - Tapping Controls navigates to `NormalNavKey.Settings.Control` screen
  - `onControlsClick` lambda threaded through `ContentMenu` and `DashboardTabBar` signatures
  - Icon updated to `ic_videogame_asset_outlined`
  