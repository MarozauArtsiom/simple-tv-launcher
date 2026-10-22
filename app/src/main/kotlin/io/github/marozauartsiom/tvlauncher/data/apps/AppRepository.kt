package io.github.marozauartsiom.tvlauncher.data.apps

import java.io.File
import java.text.Collator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The installed apps. The last known list is read from a small JSON snapshot first, so Home has
 * apps on the first frame; a fresh query follows, and install/remove events (debounced 500 ms,
 * because an update fires several in a row) trigger another one. Nothing polls.
 */
@OptIn(FlowPreview::class)
class AppRepository(
    private val source: PackageSource,
    private val snapshotFile: File,
    private val ownPackage: String,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    private val collator: () -> Collator = { Collator.getInstance() },
    private val debounceMs: Long = 500,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(TvApp.serializer())
    private val _apps = MutableStateFlow<List<TvApp>>(emptyList())
    val apps: StateFlow<List<TvApp>> = _apps.asStateFlow()

    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var unregister: (() -> Unit)? = null

    /** Loads the snapshot, queries fresh data, and starts listening for package changes. */
    fun start() {
        if (unregister != null) return
        unregister = source.registerChanges { changes.tryEmit(Unit) }
        scope.launch(io) {
            readSnapshot()?.let { if (_apps.value.isEmpty()) _apps.value = it }
            refresh()
        }
        scope.launch { changes.debounce(debounceMs).collect { refresh() } }
    }

    fun stop() {
        unregister?.invoke(); unregister = null
    }

    suspend fun refresh() {
        val fresh = withContext(io) {
            runCatching { AppLists.dedupe(source.queryLaunchable(), ownPackage, collator()) }.getOrNull()
        } ?: return // a failing query keeps the last good list
        if (fresh != _apps.value) {
            _apps.value = fresh
            withContext(io) { writeSnapshot(fresh) }
        }
    }

    private fun readSnapshot(): List<TvApp>? =
        runCatching { if (snapshotFile.exists()) json.decodeFromString(serializer, snapshotFile.readText()) else null }.getOrNull()

    private fun writeSnapshot(apps: List<TvApp>) {
        runCatching {
            val tmp = File(snapshotFile.parentFile, snapshotFile.name + ".tmp")
            tmp.writeText(json.encodeToString(serializer, apps))
            if (!tmp.renameTo(snapshotFile)) { snapshotFile.delete(); tmp.renameTo(snapshotFile) }
        }
    }
}
