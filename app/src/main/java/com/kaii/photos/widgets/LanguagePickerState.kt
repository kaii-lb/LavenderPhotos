package com.kaii.photos.widgets

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class LanguagePickerState(
    context: Context
) {
    data class Language(
        val tag: String
    ) {
        val localName: String
            get() = Locale.forLanguageTag(tag).getDisplayLanguage(
                AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
            )

        val name: String
            get() = Locale.forLanguageTag(tag).getDisplayLanguage(Locale.forLanguageTag(tag))
    }

    private val supportedLanguages = listOf(
        Language("en"),
        Language("ar"),
        Language("ca"),
        Language("cs"),
        Language("da"),
        Language("de"),
        Language("el"),
        Language("es"),
        Language("et"),
        Language("fr"),
        Language("gl"),
        Language("hy"),
        Language("id"),
        Language("it"),
        Language("ja"),
        Language("kn"),
        Language("pl"),
        Language("pt"),
        Language("pt-BR"),
        Language("ru"),
        Language("sv"),
        Language("tr"),
        Language("uk"),
        Language("vi"),
        Language("zh"),
        Language("zh-CN"),
        Language("zh-TW")
    ).sortedBy { it.tag }

    private val localeManager = context.getSystemService(LocaleManager::class.java)

    private val _languages = MutableStateFlow(supportedLanguages)
    val languages = _languages.asStateFlow()

    var query by mutableStateOf("")
        private set

    var currentLanguage by mutableStateOf(supportedLanguages.first())
        private set

    init {
        supportedLanguages.find {
            if (localeManager.applicationLocales.size() > 0) {
                it.tag == (localeManager.applicationLocales[0].language ?: Locale.getDefault().language)
            } else {
                it.tag == Locale.getDefault().language
            }
        }?.let {
            currentLanguage = it
        }
    }

    fun search(query: String) {
        this.query = query

        val lower = query.lowercase()
        _languages.value = supportedLanguages.filter {
            it.tag.lowercase().contains(lower)
                    || it.name.lowercase().contains(lower)
                    || it.localName.lowercase().contains(lower)
        }
    }

    fun choose(language: Language) {
        currentLanguage = language
        localeManager.applicationLocales = LocaleList.forLanguageTags(language.tag)
    }
}