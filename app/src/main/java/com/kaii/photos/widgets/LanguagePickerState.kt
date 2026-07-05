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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

interface LanguagePicker {
    data class Language(
        val tag: String
    ) {
        val localName: String
            get() = Locale.forLanguageTag(tag).getDisplayName(
                AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
            )

        val name: String
            get() = Locale.forLanguageTag(tag).getDisplayName(Locale.forLanguageTag(tag))
    }

    val query: String
    val currentLanguage: Language
    val languages: StateFlow<List<Language>>

    fun search(query: String)
    fun choose(language: Language)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class LanguagePickerState(
    context: Context
) : LanguagePicker {
    private val supportedLanguages = listOf(
        LanguagePicker.Language("en"),
        LanguagePicker.Language("ar"),
        LanguagePicker.Language("ca"),
        LanguagePicker.Language("cs"),
        LanguagePicker.Language("da"),
        LanguagePicker.Language("de"),
        LanguagePicker.Language("el"),
        LanguagePicker.Language("es"),
        LanguagePicker.Language("et"),
        LanguagePicker.Language("fr"),
        LanguagePicker.Language("gl"),
        LanguagePicker.Language("hy"),
        LanguagePicker.Language("id"),
        LanguagePicker.Language("it"),
        LanguagePicker.Language("ja"),
        LanguagePicker.Language("kn"),
        LanguagePicker.Language("pl"),
        LanguagePicker.Language("pt"),
        LanguagePicker.Language("pt-BR"),
        LanguagePicker.Language("ru"),
        LanguagePicker.Language("sv"),
        LanguagePicker.Language("tr"),
        LanguagePicker.Language("uk"),
        LanguagePicker.Language("vi"),
        LanguagePicker.Language("zh-CN"),
        LanguagePicker.Language("zh-TW"),
        LanguagePicker.Language("eo"),
        LanguagePicker.Language("sk")
    ).sortedBy { it.tag }

    private val localeManager = context.getSystemService(LocaleManager::class.java)

    private val _languages = MutableStateFlow(supportedLanguages)
    override val languages = _languages.asStateFlow()

    override var query by mutableStateOf("")
        private set

    override var currentLanguage by mutableStateOf(supportedLanguages.first())
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

    override fun search(query: String) {
        this.query = query

        val lower = query.lowercase()
        _languages.value = supportedLanguages.filter {
            it.tag.lowercase().contains(lower)
                    || it.name.lowercase().contains(lower)
                    || it.localName.lowercase().contains(lower)
        }
    }

    override fun choose(language: LanguagePicker.Language) {
        currentLanguage = language
        localeManager.applicationLocales = LocaleList.forLanguageTags(language.tag)
    }
}