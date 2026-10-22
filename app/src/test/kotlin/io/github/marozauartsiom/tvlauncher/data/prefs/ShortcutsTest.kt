package io.github.marozauartsiom.tvlauncher.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ShortcutsUrlTest {
    @Test fun aBareDomainGetsHttps() = assertEquals("https://news.example.com", Shortcuts.normalizeUrl("news.example.com"))

    @Test fun spacesAroundAreTrimmedAndPathsKept() =
        assertEquals("https://example.org/a/b?x=1", Shortcuts.normalizeUrl("  https://example.org/a/b?x=1 "))

    @Test fun httpIsAllowed() = assertEquals("http://192.168.1.5:8080/", Shortcuts.normalizeUrl("http://192.168.1.5:8080/"))

    @Test fun otherSchemesAreRefused() {
        listOf("javascript:alert(1)", "file:///sdcard/a.html", "intent://x#Intent;scheme=http;end", "ftp://example.com", "content://a/b", "tel:112")
            .forEach { assertNull(it, Shortcuts.normalizeUrl(it)) }
    }

    @Test fun junkIsRefused() {
        listOf("", "   ", "no spaces allowed.com", "localhostnodot", "https://", "://x.com", "http://:80")
            .forEach { assertNull("'$it'", Shortcuts.normalizeUrl(it)) }
    }

    @Test fun addressIsShownWithoutSchemeWwwAndTrailingSlash() {
        assertEquals("news.example.com", Shortcuts.displayAddress("https://www.news.example.com/"))
        assertEquals("example.org/a", Shortcuts.displayAddress("http://example.org/a"))
    }

    @Test fun idsAreUnique() {
        val taken = mutableSetOf<String>()
        repeat(200) { assertTrue(taken.add(Shortcuts.newId(taken))) }
    }
}

class ShortcutsEditorTest {
    @get:Rule val tmp = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var store: DataStore<Preferences>
    private lateinit var settings: Settings
    private lateinit var editor: ShortcutsEditor

    @Before fun setUp() {
        store = PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "settings.preferences_pb") }
        settings = Settings(store)
        editor = ShortcutsEditor(settings)
    }

    @After fun tearDown() = scope.cancel()

    private fun data() = runBlocking { settings.data.first() }

    @Test fun addStoresATrimmedNameAndANormalisedAddress() = runBlocking {
        val result = editor.save(null, "  Новости ", "news.example.com") as SaveResult.Saved
        assertEquals(listOf(result.shortcut), data().shortcuts)
        assertEquals("Новости", result.shortcut.title)
        assertEquals("https://news.example.com", result.shortcut.url)
    }

    @Test fun editKeepsTheIdAndThePlaceInTheList() = runBlocking {
        val a = (editor.save(null, "A", "a.example.com") as SaveResult.Saved).shortcut
        val b = (editor.save(null, "B", "b.example.com") as SaveResult.Saved).shortcut
        editor.save(a.id, "A2", "a2.example.com")
        assertEquals(listOf(a.id, b.id), data().shortcuts.map { it.id })
        assertEquals("A2", data().shortcuts.first().title)
        assertEquals("https://a2.example.com", data().shortcuts.first().url)
    }

    @Test fun invalidInputChangesNothing() = runBlocking {
        assertEquals(SaveResult.Invalid, editor.save(null, "", "a.example.com"))
        assertEquals(SaveResult.Invalid, editor.save(null, "A", "javascript:alert(1)"))
        assertEquals(emptyList<Shortcut>(), data().shortcuts)
    }

    @Test fun deleteRemovesTheShortcutAndItsFavouriteEntry() = runBlocking {
        val a = (editor.save(null, "A", "a.example.com") as SaveResult.Saved).shortcut
        val b = (editor.save(null, "B", "b.example.com") as SaveResult.Saved).shortcut
        settings.setFavourites(listOf(ItemKey.app("x"), ItemKey.shortcut(a.id), ItemKey.shortcut(b.id)))
        editor.delete(a.id)
        assertEquals(listOf(b.id), data().shortcuts.map { it.id })
        assertEquals(listOf(ItemKey.app("x"), ItemKey.shortcut(b.id)), data().favourites)
    }

    @Test fun twoShortcutsGetDifferentIds() = runBlocking {
        val a = (editor.save(null, "A", "a.example.com") as SaveResult.Saved).shortcut
        val b = (editor.save(null, "A", "a.example.com") as SaveResult.Saved).shortcut
        assertNotEquals(a.id, b.id)
    }

    @Test fun theListIsCapped() = runBlocking {
        repeat(Shortcuts.MAX_COUNT) { editor.save(null, "S$it", "s$it.example.com") }
        assertEquals(SaveResult.Invalid, editor.save(null, "one more", "more.example.com"))
        assertEquals(Shortcuts.MAX_COUNT, data().shortcuts.size)
    }
}
