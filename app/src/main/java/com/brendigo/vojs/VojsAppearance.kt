package com.brendigo.vojs

import android.app.Activity

/**
 * Dark is the only default on fresh installs. Existing explicit user choices
 * are preserved (light/system); we never overwrite persisted preferences.
 */
internal object VojsAppearance {
    const val DARK = "dark"
    const val LIGHT = "light"
    const val SYSTEM = "system"
    const val DEFAULT = DARK
    private const val KEY = "vojs_appearance"

    fun load(activity: Activity): String =
        activity.getPreferences(Activity.MODE_PRIVATE).getString(KEY, DEFAULT)
            ?.takeIf { it == DARK || it == LIGHT || it == SYSTEM } ?: DEFAULT

    fun save(activity: Activity, appearance: String) {
        require(appearance == DARK || appearance == LIGHT || appearance == SYSTEM)
        activity.getPreferences(Activity.MODE_PRIVATE).edit()
            .putString(KEY, appearance).apply()
    }

    fun isDark(appearance: String, systemDark: Boolean): Boolean =
        when (appearance) {
            LIGHT -> false
            SYSTEM -> systemDark
            else -> true
        }
}
