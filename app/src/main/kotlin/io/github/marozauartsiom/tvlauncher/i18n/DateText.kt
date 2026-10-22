package io.github.marozauartsiom.tvlauncher.i18n

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The long date shown next to the clock, in each language's own word order, first letter capitalised. */
object DateText {
    private val patterns = mapOf(
        "be" to "EEEE, d MMMM",
        "ru" to "EEEE, d MMMM",
        "pl" to "EEEE, d MMMM",
        "de" to "EEEE, d. MMMM",
        "fr" to "EEEE d MMMM",
        "en" to "EEEE, MMMM d",
    )

    fun format(date: LocalDate, languageTag: String): String {
        val tag = if (languageTag in patterns) languageTag else "en"
        val locale = Locale.forLanguageTag(tag)
        val text = DateTimeFormatter.ofPattern(patterns.getValue(tag), locale).format(date)
        return text.replaceFirstChar { it.titlecase(locale) }
    }
}
