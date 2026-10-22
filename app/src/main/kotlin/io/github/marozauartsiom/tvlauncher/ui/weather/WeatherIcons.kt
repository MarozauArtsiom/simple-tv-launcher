package io.github.marozauartsiom.tvlauncher.ui.weather

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import io.github.marozauartsiom.tvlauncher.data.weather.Condition

/** Weather pictures as path data (24×24, one colour; the tint comes from the theme). Night variants for clear and partly cloudy. */
object WeatherIcons {
    private const val CLOUD_LOW = "M6.5 19h11a4.5 4.5 0 0 0 .6-8.96A6 6 0 0 0 6.6 10.6 4.25 4.25 0 0 0 6.5 19z"
    private const val CLOUD_HIGH = "M6.5 15h11a4.5 4.5 0 0 0 .6-8.96A6 6 0 0 0 6.6 6.6 4.25 4.25 0 0 0 6.5 15z"
    private const val CLOUD_PARTLY = "M8.5 20h9a3.75 3.75 0 0 0 .5-7.47A5 5 0 0 0 8.6 11.9 4 4 0 0 0 8.5 20z"
    private const val MOON = "M14.5 3a8.5 8.5 0 1 0 6.5 13.9A7 7 0 0 1 14.5 3z"
    private const val SMALL_MOON = "M9.5 2.5a5.2 5.2 0 1 0 4.2 8.3 4.4 4.4 0 0 1-4.2-8.3z"

    private fun build(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    private fun ImageVector.Builder.fill(d: String, alpha: Float = 1f) =
        addPath(addPathNodes(d), fill = SolidColor(Color.Black), fillAlpha = alpha)

    private fun ImageVector.Builder.stroke(d: String) =
        addPath(addPathNodes(d), stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round)

    val Sun = build("sun") {
        fill("M12 7.5a4.5 4.5 0 1 0 0 9 4.5 4.5 0 1 0 0-9z")
        stroke("M12 2.5v2.5M12 19v2.5M2.5 12H5M19 12h2.5M5.3 5.3l1.8 1.8M16.9 16.9l1.8 1.8M5.3 18.7l1.8-1.8M16.9 7.1l1.8-1.8")
    }
    val Moon = build("moon") { fill(MOON) }
    val Partly = build("partly") { fill("M8 4.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 1 0 0-7z", 0.7f); fill(CLOUD_PARTLY) }
    val PartlyNight = build("partly_night") { fill(SMALL_MOON, 0.8f); fill(CLOUD_PARTLY) }
    val Cloud = build("cloud") { fill(CLOUD_LOW) }
    val Rain = build("rain") { fill(CLOUD_HIGH); stroke("M8 17.5l-1 3M12 17.5l-1 3M16 17.5l-1 3") }
    val Snow = build("snow") {
        fill(CLOUD_HIGH)
        fill("M8 17.6a1.1 1.1 0 1 0 0 2.2 1.1 1.1 0 1 0 0-2.2zM12 18.6a1.1 1.1 0 1 0 0 2.2 1.1 1.1 0 1 0 0-2.2zM16 17.6a1.1 1.1 0 1 0 0 2.2 1.1 1.1 0 1 0 0-2.2z")
    }
    val Fog = build("fog") { stroke("M3.5 8h17M3.5 12h17M6.5 16h11") }
    val Thunder = build("thunder") { fill(CLOUD_HIGH); fill("M12.8 14.2l-3.4 4.6h2.6l-1 3.4 4.2-5.6h-2.6l1-2.4z") }

    fun of(condition: Condition, isDay: Boolean): ImageVector = when (condition) {
        Condition.Clear -> if (isDay) Sun else Moon
        Condition.MainlyClear -> if (isDay) Partly else PartlyNight
        Condition.PartlyCloudy -> if (isDay) Partly else PartlyNight
        Condition.Overcast -> Cloud
        Condition.Fog -> Fog
        Condition.Drizzle, Condition.Rain, Condition.Showers -> Rain
        Condition.Snow, Condition.SnowShowers -> Snow
        Condition.Thunderstorm -> Thunder
    }
}
