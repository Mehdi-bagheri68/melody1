package com.example.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.model.AppLanguage

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.FA }

object AppStrings {
    fun tr(lang: AppLanguage, fa: String, en: String): String =
        if (lang == AppLanguage.FA) fa else en
}
