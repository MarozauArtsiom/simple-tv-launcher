package io.github.marozauartsiom.tvlauncher.ui.home

import io.github.marozauartsiom.tvlauncher.data.prefs.Favourites
import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
import io.github.marozauartsiom.tvlauncher.data.prefs.Settings
import kotlinx.coroutines.flow.first

/**
 * Changes to Favourites made from Home: add, remove, move one step, undo a move. Every change is
 * written to the store at once, so the order survives a restart even if the owner never presses OK.
 */
class FavouritesEditor(private val settings: Settings) {
    suspend fun current(): List<ItemKey> = settings.data.first().favourites

    suspend fun setFavourite(key: ItemKey, favourite: Boolean) {
        if (favourite) settings.addFavourite(key) else settings.removeFavourite(key)
    }

    /**
     * Swaps [key] with its neighbour in the row the owner sees ([visible]: hidden and uninstalled
     * items are not in it). The stored list may hold more keys than that.
     */
    suspend fun step(key: ItemKey, delta: Int, visible: List<ItemKey>) {
        settings.updateFavourites { stored -> moveBy(stored, visible, key, delta) }
    }

    /** Puts back the order saved when the move started (Back in move mode). */
    suspend fun restore(order: List<ItemKey>) = settings.setFavourites(order)

    companion object {
        fun moveBy(stored: List<ItemKey>, visible: List<ItemKey>, key: ItemKey, delta: Int): List<ItemKey> {
            val from = visible.indexOf(key)
            val to = from + delta
            if (from < 0 || to !in visible.indices) return stored
            val toIndex = stored.indexOf(visible[to])
            return if (toIndex < 0) stored else Favourites.move(stored, key, toIndex)
        }
    }
}
