package io.github.marozauartsiom.tvlauncher.data.prefs

import java.net.URI
import java.util.UUID
import kotlinx.coroutines.flow.first

/** Rules for website shortcuts: which addresses are accepted, and how they are shown. */
object Shortcuts {
    const val MAX_TITLE = 40
    const val MAX_COUNT = 60

    /**
     * Turns what the owner typed ("news.example.com") into an address that can be opened
     * (`https://news.example.com`), or null. Only http and https with a real host are accepted, so a
     * shortcut can never carry an `intent:`, `file:`, `javascript:` or similar address.
     */
    fun normalizeUrl(input: String): String? {
        val text = input.trim()
        if (text.isEmpty() || text.any { it.isWhitespace() }) return null
        val withScheme = if ("://" in text) text else "https://$text"
        val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https")) return null
        val host = uri.host ?: return null
        if ('.' !in host && host != "localhost") return null
        return withScheme
    }

    /** "https://www.news.example.com/" shown as "news.example.com". */
    fun displayAddress(url: String): String =
        url.substringAfter("://").removePrefix("www.").trimEnd('/')

    fun newId(taken: Set<String>): String {
        while (true) {
            val id = UUID.randomUUID().toString().take(8)
            if (id !in taken) return id
        }
    }
}

sealed interface SaveResult {
    data class Saved(val shortcut: Shortcut) : SaveResult
    data object Invalid : SaveResult
}

/** Add, edit and delete shortcuts. Deleting also takes the shortcut out of Favourites. */
class ShortcutsEditor(private val settings: Settings) {
    /** [id] is null for a new shortcut. */
    suspend fun save(id: String?, title: String, address: String): SaveResult {
        val name = title.trim().take(Shortcuts.MAX_TITLE)
        val url = Shortcuts.normalizeUrl(address)
        if (name.isEmpty() || url == null) return SaveResult.Invalid
        val current = settings.data.first().shortcuts
        if (id == null && current.size >= Shortcuts.MAX_COUNT) return SaveResult.Invalid
        val shortcut = Shortcut(id ?: Shortcuts.newId(current.map { it.id }.toSet()), name, url)
        settings.setShortcuts(if (id != null && current.any { it.id == id }) current.map { if (it.id == id) shortcut else it } else current + shortcut)
        return SaveResult.Saved(shortcut)
    }

    suspend fun delete(id: String) {
        settings.setShortcuts(settings.data.first().shortcuts.filterNot { it.id == id })
        settings.removeFavourite(ItemKey.shortcut(id))
    }
}
