package com.eobme.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    data class LanguageOption(
        val code: String,
        val displayName: String,
        val nativeName: String
    )

    val supportedLanguages = listOf(
        LanguageOption("en", "English", "English"),
        LanguageOption("es", "Spanish", "Español"),
        LanguageOption("fr", "French", "Français"),
        LanguageOption("zh", "Chinese", "中文")
    )

    fun setLocale(context: Context, languageCode: String): Context {
        @Suppress("DEPRECATION")
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
