package com.actiary.data.appearance

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

enum class AppLanguage(val storageKey: String, val languageTag: String?) {
    SYSTEM("SYSTEM", null),
    ZH("ZH", "zh-CN"),
    EN("EN", "en");

    companion object {
        fun fromStorage(raw: String?): AppLanguage =
            entries.firstOrNull { it.storageKey == raw } ?: SYSTEM
    }
}

internal object AppLanguagePrefs {
    private const val PREFS_NAME = "actiary_language"
    private const val KEY_LANGUAGE = "app_language"

    fun load(context: Context): AppLanguage {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
        return AppLanguage.fromStorage(raw)
    }

    fun save(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.storageKey)
            .apply()
    }

    fun wrapContext(base: Context): Context {
        val tag = load(base).languageTag ?: return base
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }
}
