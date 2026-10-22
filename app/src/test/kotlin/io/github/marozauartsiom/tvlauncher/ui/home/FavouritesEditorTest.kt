package io.github.marozauartsiom.tvlauncher.ui.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
import io.github.marozauartsiom.tvlauncher.data.prefs.Settings
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FavouritesEditorTest {
    @get:Rule val tmp = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var store: DataStore<Preferences>
    private lateinit var settings: Settings
    private lateinit var editor: FavouritesEditor

    private val a = ItemKey.app("a")
    private val b = ItemKey.app("b")
    private val c = ItemKey.app("c")
    private val hidden = ItemKey.app("hidden")

    @Before fun setUp() {
        store = PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "settings.preferences_pb") }
        settings = Settings(store)
        editor = FavouritesEditor(settings)
    }

    @After fun tearDown() = scope.cancel()

    private fun stored() = runBlocking { settings.data.first().favourites }

    @Test fun addingAppendsAndRemovingKeepsTheRestInOrder() = runBlocking {
        editor.setFavourite(a, true); editor.setFavourite(b, true); editor.setFavourite(c, true)
        assertEquals(listOf(a, b, c), stored())
        editor.setFavourite(b, false)
        assertEquals(listOf(a, c), stored())
    }

    @Test fun addingTwiceDoesNotDuplicate() = runBlocking {
        editor.setFavourite(a, true); editor.setFavourite(a, true)
        assertEquals(listOf(a), stored())
    }

    @Test fun stepSwapsWithTheNeighbourAndSurvivesAReload() = runBlocking {
        settings.setFavourites(listOf(a, b, c))
        editor.step(a, +1, visible = listOf(a, b, c))
        assertEquals(listOf(b, a, c), stored())
        editor.step(c, -1, visible = listOf(b, a, c))
        assertEquals(listOf(b, c, a), stored())
        // a new editor (like after a restart) sees the same order
        assertEquals(listOf(b, c, a), FavouritesEditor(Settings(store)).current())
    }

    @Test fun stepAtTheEndsDoesNothing() = runBlocking {
        settings.setFavourites(listOf(a, b))
        editor.step(a, -1, visible = listOf(a, b))
        editor.step(b, +1, visible = listOf(a, b))
        assertEquals(listOf(a, b), stored())
    }

    @Test fun stepSkipsHiddenFavouritesThatStayInStorage() = runBlocking {
        settings.setFavourites(listOf(a, hidden, b))
        editor.step(a, +1, visible = listOf(a, b)) // the owner sees a, b
        assertEquals(listOf(hidden, b, a), stored())
    }

    @Test fun restoreBringsBackTheOrderFromBeforeTheMove() = runBlocking {
        settings.setFavourites(listOf(a, b, c))
        val before = editor.current()
        editor.step(a, +1, visible = listOf(a, b, c))
        editor.step(a, +1, visible = listOf(b, a, c))
        assertEquals(listOf(b, c, a), stored())
        editor.restore(before)
        assertEquals(listOf(a, b, c), stored())
    }
}
