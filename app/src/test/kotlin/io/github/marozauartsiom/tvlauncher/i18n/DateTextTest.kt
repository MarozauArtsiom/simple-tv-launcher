package io.github.marozauartsiom.tvlauncher.i18n

import io.github.marozauartsiom.tvlauncher.data.prefs.LanguageChoice
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTextTest {
    private val day = LocalDate.of(2026, 10, 21) // a Wednesday

    @Test fun datesUseEachLanguagesWordOrderAndCapitalise() {
        assertEquals("Серада, 21 кастрычніка", DateText.format(day, "be"))
        assertEquals("Среда, 21 октября", DateText.format(day, "ru"))
        assertEquals("Środa, 21 października", DateText.format(day, "pl"))
        assertEquals("Mittwoch, 21. Oktober", DateText.format(day, "de"))
        assertEquals("Mercredi 21 octobre", DateText.format(day, "fr"))
        assertEquals("Wednesday, October 21", DateText.format(day, "en"))
    }

    @Test fun unknownLanguageFallsBackToEnglishOrder() {
        assertEquals("Wednesday, October 21", DateText.format(day, "xx"))
    }

    @Test fun followTheTvUsesItsLanguageWhenShippedElseEnglish() {
        assertEquals("pl", LocaleController.effectiveTag(LanguageChoice.Follow, Locale("pl", "PL")))
        assertEquals("be", LocaleController.effectiveTag(LanguageChoice.Follow, Locale("be")))
        assertEquals("en", LocaleController.effectiveTag(LanguageChoice.Follow, Locale("ja", "JP")))
        assertEquals("en", LocaleController.effectiveTag(LanguageChoice.Follow, Locale("uk")))
        assertEquals("de", LocaleController.effectiveTag(LanguageChoice.Fixed("de"), Locale("ja")))
    }
}
