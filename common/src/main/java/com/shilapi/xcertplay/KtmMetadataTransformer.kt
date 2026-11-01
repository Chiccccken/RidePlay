package com.shilapi.xcertplay

import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Locale

/** Local, bounded conversion of Han text only. Original metadata is never mutated. */
internal object KtmMetadataTransformer {
    private val kana = Regex("[\\p{IsHiragana}\\p{IsKatakana}]")
    private val han = Regex("\\p{IsHan}+")
    private val dictionary = mapOf(
        "重庆" to "Chong Qing", "重慶" to "Chong Qing",
        "银行" to "Yin Hang", "銀行" to "Yin Hang",
        "音乐" to "Yin Yue", "音樂" to "Yin Yue",
        "长安" to "Chang An", "長安" to "Chang An",
        "单依纯" to "Shan Yi Chun", "曾轶可" to "Zeng Yi Ke",
    )
    private val phrases = Regex(dictionary.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) })
    private val cache = object : LinkedHashMap<String, String>(128, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 128
    }
    @Synchronized fun convert(value: String?): String? {
        if (value == null || !han.containsMatchIn(value) || kana.containsMatchIn(value) || Build.VERSION.SDK_INT < 29) return value
        return cache[value] ?: runCatching { convertSupported(value) }.getOrDefault(value).also { cache[value] = it }
    }
    @RequiresApi(29)
    private fun convertSupported(value: String): String = buildString {
        var end = 0
        for (match in han.findAll(value)) {
            append(value.substring(end, match.range.first))
            if (isNotEmpty() && last().isLetterOrDigit()) append(' ')
            val latin = buildString {
                var start = 0
                for (phrase in phrases.findAll(match.value)) {
                    append(Engine.transliterator.transliterate(match.value.substring(start, phrase.range.first)))
                    append(' '); append(dictionary.getValue(phrase.value)); append(' ')
                    start = phrase.range.last + 1
                }
                append(Engine.transliterator.transliterate(match.value.substring(start)))
            }
            val ascii = java.text.Normalizer.normalize(latin, java.text.Normalizer.Form.NFD)
                .replace("u\u0308", "v").replace("U\u0308", "V")
                .replace(Regex("\\p{M}+"), "")
            append(ascii.split(Regex("\\s+")).filter(String::isNotEmpty).joinToString(" ") {
                it.lowercase(Locale.ROOT).replaceFirstChar { c -> c.titlecase(Locale.ROOT) }
            })
            end = match.range.last + 1
            if (end < value.length && value[end].isLetterOrDigit()) append(' ')
        }
        append(value.substring(end))
    }
    @RequiresApi(29)
    private object Engine {
        val transliterator = android.icu.text.Transliterator.getInstance("Han-Latin")
    }
}
