package io.github.marozauartsiom.tvlauncher.i18n

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Every language file must have the same keys, the same placeholders and complete plural forms as English. */
class StringsConsistencyTest {
    private val res = File("src/main/res")
    private val languages = listOf("ru", "be", "pl", "de", "fr")
    private val placeholder = Regex("""%\d+\$[sd]""")

    private data class Entry(val text: String?, val plurals: Map<String, String>?)

    private fun parse(folder: String): Map<String, Entry> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$folder/strings.xml"))
        val out = mutableMapOf<String, Entry>()
        val nodes = doc.documentElement.childNodes
        for (i in 0 until nodes.length) {
            val e = nodes.item(i) as? Element ?: continue
            when (e.tagName) {
                "string" -> if (e.getAttribute("translatable") != "false") out[e.getAttribute("name")] = Entry(e.textContent, null)
                "plurals" -> {
                    val items = e.getElementsByTagName("item")
                    out[e.getAttribute("name")] = Entry(null, (0 until items.length).associate {
                        val item = items.item(it) as Element; item.getAttribute("quantity") to item.textContent
                    })
                }
            }
        }
        return out
    }

    private val english = parse("values")

    @Test fun englishHasStrings() = assertTrue(english.size > 100)

    @Test fun everyLanguageHasExactlyTheEnglishKeys() {
        for (lang in languages) {
            val other = parse("values-$lang")
            assertEquals("$lang is missing keys", emptySet<String>(), english.keys - other.keys)
            assertEquals("$lang has extra keys", emptySet<String>(), other.keys - english.keys)
        }
    }

    @Test fun placeholdersMatchEnglish() {
        for (lang in languages) {
            val other = parse("values-$lang")
            for ((key, en) in english) {
                val a = placeholdersOf(en); val b = placeholdersOf(other.getValue(key))
                assertEquals("$lang/$key placeholders", a, b)
            }
        }
    }

    private fun placeholdersOf(e: Entry): List<String> =
        (e.plurals?.values?.flatMap { placeholder.findAll(it).map { m -> m.value }.toList().sorted().distinct() } ?: placeholder.findAll(e.text!!).map { it.value }.toList())
            .sorted().distinct()

    @Test fun pluralFormsAreCompleteForEachLanguage() {
        val required = mapOf(
            "en" to setOf("one", "other"), "de" to setOf("one", "other"),
            "fr" to setOf("one", "many", "other"),
            "ru" to setOf("one", "few", "many", "other"), "be" to setOf("one", "few", "many", "other"), "pl" to setOf("one", "few", "many", "other"),
        )
        for ((lang, folder) in listOf("en" to "values") + languages.map { it to "values-$it" }) {
            parse(folder).filterValues { it.plurals != null }.forEach { (key, e) ->
                assertEquals("$lang/$key plural forms", required.getValue(lang), e.plurals!!.keys)
            }
        }
    }

    @Test fun noBlankValuesAndNoUnescapedApostrophes() {
        for (folder in listOf("values") + languages.map { "values-$it" }) {
            val raw = File(res, "$folder/strings.xml").readText()
            assertFalse("$folder has an unescaped apostrophe", Regex("""(?<!\\)'""").containsMatchIn(raw.substringAfter("<resources>")))
            parse(folder).forEach { (key, e) ->
                (e.text?.let { listOf(it) } ?: e.plurals!!.values).forEach { assertTrue("$folder/$key is blank", it.isNotBlank()) }
            }
        }
    }

    @Test fun belarusianAndRussianDifferWhereTheyShould() {
        // Guards against a copy-paste of the Russian file into Belarusian.
        val ru = parse("values-ru"); val be = parse("values-be")
        assertEquals("Абранае", be.getValue("favourites").text)
        assertEquals("Избранное", ru.getValue("favourites").text)
        assertTrue(be.count { (k, v) -> v.text != null && v.text != ru.getValue(k).text } > 80)
    }
}
