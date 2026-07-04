# Build Trigger

  This file triggers the final GitHub Actions APK build.

  ## Kopper Zink built-in renderer

  Added Kopper Zink as a fully integrated built-in renderer:
  - New file: KopperZinkRenderer.kt (rendererId: opengles3_desktopgl_zink_kopper)
  - Registered in Renderers.kt after VulkanZinkRenderer
  - kopper-zink-release.aar added to libs/ (provides libglxshim.so + libEGL_mesa.so)
  - Appears in Settings → Renderer → Built-in Renderers tab
  - Persists selection and integrates with existing renderer switching/lifecycle
  - Automatically excluded on 32-bit x86 devices (no Zink binary)
  - No regressions to existing renderers or launcher functionality
  