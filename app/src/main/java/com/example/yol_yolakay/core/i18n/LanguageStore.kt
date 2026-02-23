package com.example.yol_yolakay.core.i18n

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.langDataStore by preferencesDataStore("app_language_store")

class LanguageStore(private val appContext: Context) {

    private object Keys {
        val LANG = stringPreferencesKey("app_language") // "uz" | "ru" | "en"
    }

    val languageFlow: Flow<String?> =
        appContext.langDataStore.data.map { it[Keys.LANG] }

    suspend fun get(): String? =
        appContext.langDataStore.data.first()[Keys.LANG]

    suspend fun set(code: String) {
        val normalized = normalize(code)
        appContext.langDataStore.edit { it[Keys.LANG] = normalized }
    }

    companion object {
        fun apply(code: String) {
            val tag = normalize(code)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }

        fun clear() {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }

        private fun normalize(code: String): String =
            when (code.trim().lowercase()) {
                "uz" -> "uz"
                "ru" -> "ru"
                "en" -> "en"
                else -> "uz"
            }
    }
}