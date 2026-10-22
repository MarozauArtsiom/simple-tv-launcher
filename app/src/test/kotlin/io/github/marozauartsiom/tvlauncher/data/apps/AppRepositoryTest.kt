package io.github.marozauartsiom.tvlauncher.data.apps

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

class FakeSource(var apps: List<TvApp> = emptyList()) : PackageSource {
    var queries = 0
    var fail = false
    var onChange: (() -> Unit)? = null
    var intents = mutableMapOf<String, Intent?>()
    override fun queryLaunchable(): List<TvApp> { queries++; if (fail) error("binder died"); return apps }
    override fun loadArt(app: TvApp): LoadedArt? = null
    override fun launchIntent(packageName: String): Intent? = intents[packageName]
    override fun registerChanges(onChange: () -> Unit): () -> Unit { this.onChange = onChange; return { this.onChange = null } }
}

fun app(pkg: String, label: String = pkg, leanback: Boolean = true, cls: String = "$pkg.Main") = TvApp(pkg, cls, label, leanback)

class AppListsTest {
    private val c: Collator = Collator.getInstance(Locale.ENGLISH)

    @Test fun leanbackWinsAndEachPackageAppearsOnce() {
        val list = AppLists.dedupe(listOf(app("a", "A phone", leanback = false, cls = "a.Phone"), app("a", "A tv", cls = "a.Tv"), app("b", "B")), "own", c)
        assertEquals(listOf("a.Tv", "b.Main"), list.map { it.activityClass })
    }

    @Test fun ourOwnPackagesAreExcludedIncludingTheDebugSuffix() {
        val list = AppLists.dedupe(listOf(app("own"), app("own.debug"), app("x")), "own", c)
        assertEquals(listOf("x"), list.map { it.packageName })
    }

    @Test fun hiddenAppsAreFiltered() {
        assertEquals(listOf("b"), AppLists.visible(listOf(app("a"), app("b")), setOf("a")).map { it.packageName })
    }

    @Test fun usageSortThenAlphabetical() {
        val apps = listOf(app("c", "Cinema"), app("a", "Alpha"), app("b", "Beta"), app("z", "Zed"))
        val sorted = AppLists.byUsage(apps, mapOf("pkg:z" to 5, "pkg:b" to 2, "pkg:c" to 2), Locale.ENGLISH)
        assertEquals(listOf("z", "b", "c", "a"), sorted.map { it.packageName })
    }

    @Test fun cyrillicLabelsSortWithTheirLocale() {
        val sorted = AppLists.byUsage(listOf(app("1", "Яндекс"), app("2", "Кинопоиск"), app("3", "Амедиатека")), emptyMap(), Locale.forLanguageTag("ru"))
        assertEquals(listOf("Амедиатека", "Кинопоиск", "Яндекс"), sorted.map { it.label })
    }
}

/** `advanceUntilIdle` ignores background work, so move virtual time explicitly. */
@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.settle() { advanceTimeBy(1_000); runCurrent() }

@OptIn(ExperimentalCoroutinesApi::class)
class AppRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val file get() = File(tmp.root, "apps.json")

    private fun TestScope.repo(source: FakeSource) =
        AppRepository(source, file, "own", backgroundScope, StandardTestDispatcher(testScheduler), { Collator.getInstance(Locale.ENGLISH) })

    @Test fun startQueriesAndSavesASnapshot() = runTest {
        val source = FakeSource(listOf(app("b", "Beta"), app("a", "Alpha")))
        val repo = repo(source); repo.start(); settle()
        assertEquals(listOf("a", "b"), repo.apps.value.map { it.packageName })
        assertTrue(file.exists())
    }

    @Test fun anExistingSnapshotIsShownBeforeTheQueryResult() = runTest {
        file.writeText("""[{"packageName":"old","activityClass":"old.Main","label":"Old","isLeanback":true}]""")
        val source = FakeSource(emptyList()).also { it.fail = true } // the query fails: the snapshot stays
        val repo = repo(source); repo.start(); settle()
        assertEquals(listOf("old"), repo.apps.value.map { it.packageName })
    }

    @Test fun aCorruptSnapshotIsIgnored() = runTest {
        file.writeText("{{{ not json")
        val repo = repo(FakeSource(listOf(app("a")))); repo.start(); settle()
        assertEquals(listOf("a"), repo.apps.value.map { it.packageName })
    }

    @Test fun aBurstOfPackageEventsCausesOneRefresh() = runTest {
        val source = FakeSource(listOf(app("a")))
        val repo = repo(source); repo.start(); settle()
        val before = source.queries
        source.apps = listOf(app("a"), app("b"))
        repeat(5) { source.onChange!!(); advanceTimeBy(100) }
        settle()
        assertEquals(before + 1, source.queries)
        assertEquals(2, repo.apps.value.size)
    }

    @Test fun stopUnregistersTheCallback() = runTest {
        val source = FakeSource(); val repo = repo(source); repo.start(); settle()
        repo.stop(); assertNull(source.onChange)
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AppLauncherTest {
    private val source = FakeSource()

    @Test fun missingIntentMeansNotFound() = assertEquals(LaunchResult.NotFound, AppLauncher(source) { }.launch("x"))

    @Test fun startsWithNewTaskFlags() {
        source.intents["x"] = Intent("test")
        var started: Intent? = null
        assertEquals(LaunchResult.Started, AppLauncher(source) { started = it }.launch("x"))
        assertTrue(started!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test fun platformErrorsAreMapped() {
        source.intents["x"] = Intent("test")
        assertEquals(LaunchResult.NotFound, AppLauncher(source) { throw ActivityNotFoundException() }.launch("x"))
        assertEquals(LaunchResult.Failed, AppLauncher(source) { throw SecurityException() }.launch("x"))
    }
}
