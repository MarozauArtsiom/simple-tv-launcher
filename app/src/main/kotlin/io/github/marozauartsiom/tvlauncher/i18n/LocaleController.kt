package io.github.marozauartsiom.tvlauncher.i18n

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import io.github.marozauartsiom.tvlauncher.data.prefs.LanguageChoice
import java.util.Locale

/**
 * The launcher's own language, independent of the TV's. The DataStore is the owner of the setting;
 * this class keeps a tiny mirror in SharedPreferences because the language must be known
 * synchronously in `attachBaseContext`, before any coroutine can read the DataStore.
 *
 * - API 33+: the platform's per-app locale (`LocaleManager`).
 * - Older: the base context is wrapped with the chosen locale and the activity is recreated.
 * - "Same as the TV": no override. Android picks the TV's language if we ship it, else English.
 */
class LocaleController(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("locale_boot", Context.MODE_PRIVATE)

    fun savedChoice(): LanguageChoice =
        prefs.getString(KEY, null)?.takeIf { it in LanguageChoice.supportedTags }?.let { LanguageChoice.Fixed(it) } ?: LanguageChoice.Follow

    /** Call from `Activity.attachBaseContext`. */
    fun wrap(base: Context): Context = localized(base, savedChoice())

    /** Persists the choice and applies it right away. [activity] is recreated on API < 33. */
    fun apply(choice: LanguageChoice, activity: Activity?) {
        prefs.edit().apply {
            if (choice is LanguageChoice.Fixed) putString(KEY, choice.tag) else remove(KEY)
        }.apply()
        if (Build.VERSION.SDK_INT >= 33) {
            val tags = (choice as? LanguageChoice.Fixed)?.tag.orEmpty()
            appContext.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tags)
        } else {
            activity?.recreate()
        }
    }

    companion object {
        private const val KEY = "language"

        /** A context whose resources use [choice]'s language. Below API 33 only; on 33+ the platform does it. */
        fun localized(base: Context, choice: LanguageChoice): Context {
            if (choice !is LanguageChoice.Fixed || Build.VERSION.SDK_INT >= 33) return base
            return localizedTo(base, choice.tag)
        }

        /** Always wraps, whatever the API level (used by tests and previews). */
        fun localizedTo(base: Context, tag: String): Context {
            val locale = Locale.forLanguageTag(tag)
            val config = Configuration(base.resources.configuration).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
            return base.createConfigurationContext(config)
        }

        /**
         * The language the UI will actually use: the fixed choice, or the TV's language when we
         * ship it, or English.
         */
        fun effectiveTag(choice: LanguageChoice, systemLocale: Locale): String = when (choice) {
            is LanguageChoice.Fixed -> choice.tag
            LanguageChoice.Follow -> systemLocale.language.takeIf { it in LanguageChoice.supportedTags } ?: "en"
        }
    }
}
