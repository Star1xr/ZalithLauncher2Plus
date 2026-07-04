# Build Trigger

  This file triggers the final GitHub Actions APK build.

  ## Ely.by Account Authentication

  Added Ely.by login as a built-in login option beside Offline Login:
  - Added URL_ELY_BY_AUTH constant to UrlManager.kt (https://account.ely.by/api/authlib-injector/)
  - Added onElyByLogin parameter and Ely.by button to LoginMenuDialog in AccountElements.kt
  - Wired onElyByLogin handler in AccountManageScreen.kt

  Implementation reuses the existing Yggdrasil/auth-server infrastructure:
  - AuthServerHelper handles authentication via the authlib-injector protocol
  - OtherLoginOperation manages the login flow (login, error, role select)
  - AuthServer data class carries the Ely.by endpoint and registration URL
  - No new account type or duplicate authentication code
  - Session persistence, logout, account switching all handled by existing code

  The Ely.by button appears beside Offline Login in the login menu dialog,
  following the same Material 3 style as other login options.
  