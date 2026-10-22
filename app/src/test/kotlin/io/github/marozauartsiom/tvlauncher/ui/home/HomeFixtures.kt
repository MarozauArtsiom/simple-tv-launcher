package io.github.marozauartsiom.tvlauncher.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.asImageBitmap
import io.github.marozauartsiom.tvlauncher.data.apps.ArtSource
import io.github.marozauartsiom.tvlauncher.data.apps.BannerArt
import io.github.marozauartsiom.tvlauncher.data.apps.TvApp
import io.github.marozauartsiom.tvlauncher.data.apps.app
import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
import io.github.marozauartsiom.tvlauncher.data.prefs.SettingsData

/** The apps of `design/mockups/home.html`, with the same tile colours. [generated] have no banner of their own. */
object HomeFixtures {
    private val colours = mapOf(
        "YouTube" to 0xFF7A2E2E, "Кинопоиск" to 0xFF5A3A1A, "TV Bro" to 0xFF2C4A6B, "Spotify" to 0xFF24523A,
        "Plex" to 0xFF5C4A1F, "VLC" to 0xFF6B3F1C, "Disney+" to 0xFF203C5C, "Google Play" to 0xFF2F4A3A,
        "Kodi" to 0xFF25485A, "Netflix" to 0xFF6B1F24, "Prime Video" to 0xFF1F4A5C, "Telegram" to 0xFF3A3358,
        "Twitch" to 0xFF43305E, "Файлы" to 0xFF3B3F4A,
        "Google Play Фильмы" to 0xFF5C2A3A, "Xiaomi Mi Home" to 0xFF2A4A5C,
    )
    fun colourOf(name: String): Long? = colours[name]
    val generated = setOf("Kodi", "Telegram")
    val favouriteNames = listOf("YouTube", "Кинопоиск", "TV Bro", "Spotify", "Plex", "VLC")

    /** The two apps the Hidden apps mockup lists. */
    val hiddenNames = setOf("Google Play Фильмы", "Xiaomi Mi Home")
    val hiddenApps: List<TvApp> = hiddenNames.map { name -> app(pkg(name), label = name) }

    /** The apps on Home in the mockup. */
    val apps: List<TvApp> = colours.keys.filterNot { it in hiddenNames }.map { name -> app(pkg(name), label = name) }
    fun pkg(name: String) = "com.example." + name.lowercase().filter { it.isLetterOrDigit() }.ifEmpty { "app${name.hashCode()}" }
    fun key(name: String) = ItemKey.app(pkg(name))

    val settings = SettingsData(favourites = favouriteNames.map(::key))
    val content: HomeContent = HomeLists.build(apps, settings, locale = java.util.Locale.ENGLISH)

    val topBar = TopBarState(
        weather = WeatherChip("+7°", "Минск", "+7°"),
        hdmi = listOf(HdmiChip("hdmi1", "HDMI 1", false), HdmiChip("hdmi2", "HDMI 2", true)),
    )
    val clock = ClockText("20:41", "Среда, 21 октября")
}

/** Draws a 320x180 tile in the app's colour with its name, like a real banner would carry. */
class FakeArt : ArtSource {
    private val cache = HashMap<String, BannerArt>()

    override fun peek(app: TvApp): BannerArt? = cache.getOrPut(app.packageName) { render(app) }
    override suspend fun load(app: TvApp): BannerArt = peek(app)!!

    private fun render(app: TvApp): BannerArt {
        val generated = app.label in HomeFixtures.generated
        val bitmap = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor((HomeFixtures.colourOf(app.label) ?: 0xFF3B3F4A).toInt())
        if (generated) {
            canvas.drawRoundRect(124f, 32f, 196f, 104f, 20f, 20f, Paint().apply { color = AndroidColor.argb(217, 255, 255, 255) })
        } else {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.rgb(0xF4, 0xF5, 0xF8); textSize = 32f; textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(app.label, 160f, 100f, paint)
        }
        return BannerArt(bitmap.asImageBitmap(), isGenerated = generated)
    }
}
