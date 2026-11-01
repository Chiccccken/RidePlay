package com.shilapi.xcertplay

import android.content.Context
import org.json.JSONObject

/** Literal whole-name replacements, stored only in the app's local preferences. */
internal object ArtistNameOverrides {
    const val MAX_RULES = 100
    const val MAX_NAME = 200
    private const val PREFS = "rideplay_artist_names"
    fun load(context: Context): Map<String, String> = runCatching {
        val json = JSONObject(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("rules", "{}")!!)
        json.keys().asSequence().take(MAX_RULES).mapNotNull { key ->
            val value = json.opt(key) as? String
            if (key.isNotBlank() && key.length <= MAX_NAME && !value.isNullOrBlank() && value.length <= MAX_NAME)
                key to value else null
        }.toMap()
    }.getOrDefault(emptyMap())
    fun save(context: Context, rules: Map<String, String>) {
        require(rules.size <= MAX_RULES)
        require(rules.all { (key, value) -> key.isNotBlank() && value.isNotBlank() && key.length <= MAX_NAME && value.length <= MAX_NAME })
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("rules", JSONObject(rules).toString()).apply()
    }
    fun displayed(original: String?, rules: Map<String, String>): String? =
        original?.let { rules[it.trim()] } ?: KtmMetadataTransformer.convert(original)
}
