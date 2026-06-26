package com.movtery.zalithlauncher.game.control.legacy

import org.json.JSONException
import org.json.JSONObject

data class LegacyControlInfo(
    val name: String = "",
    val version: String = "",
    val author: String = "",
    val desc: String = ""
)

data class LegacyCustomControls(
    val formatVersion: Int,
    val scaledAt: Float,
    val info: LegacyControlInfo,
    val buttonCount: Int,
    val drawerCount: Int,
    val joystickCount: Int
)

object LegacyControlParser {

    fun isLegacyFormat(jsonString: String): Boolean {
        return try {
            val json = JSONObject(jsonString)
            json.has("mControlDataList") || json.has("mControlInfoDataList") ||
                (json.has("scaledAt") && (json.has("mControlDataList") || json.has("mJoystickDataList")))
        } catch (_: JSONException) {
            false
        }
    }

    fun parse(jsonString: String): LegacyCustomControls? {
        return try {
            val json = JSONObject(jsonString)
            val formatVersion = if (json.has("version")) json.getInt("version") else 1
            val scaledAt = if (json.has("scaledAt")) json.getDouble("scaledAt").toFloat() else 100f

            val infoJson = json.optJSONObject("mControlInfoDataList")
            val info = if (infoJson != null) {
                LegacyControlInfo(
                    name    = infoJson.optString("name",    "").sanitize(),
                    version = infoJson.optString("version", "").sanitize(),
                    author  = infoJson.optString("author",  "").sanitize(),
                    desc    = infoJson.optString("desc",    "").sanitize()
                )
            } else {
                LegacyControlInfo()
            }

            val buttonCount  = json.optJSONArray("mControlDataList")?.length() ?: 0
            val drawerCount  = json.optJSONArray("mDrawerDataList")?.length() ?: 0
            val joystickCount = json.optJSONArray("mJoystickDataList")?.length() ?: 0

            LegacyCustomControls(
                formatVersion = formatVersion,
                scaledAt = scaledAt,
                info = info,
                buttonCount = buttonCount,
                drawerCount = drawerCount,
                joystickCount = joystickCount
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Returns empty string for "null" literal, blank, or actual empty values. */
    private fun String.sanitize(): String = if (this == "null" || isBlank()) "" else this

    fun updateInfo(jsonString: String, newInfo: LegacyControlInfo): String {
        val json = JSONObject(jsonString)
        val infoJson = json.optJSONObject("mControlInfoDataList") ?: JSONObject()
        infoJson.put("name",    newInfo.name)
        infoJson.put("version", newInfo.version)
        infoJson.put("author",  newInfo.author)
        infoJson.put("desc",    newInfo.desc)
        json.put("mControlInfoDataList", infoJson)
        return json.toString(2)
    }
}
