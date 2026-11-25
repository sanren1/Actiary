package com.actiary.ui.settings

import androidx.annotation.StringRes
import com.actiary.R

internal data class NoticeLink(@StringRes val label: Int, val url: String)

internal data class ThirdPartyProject(
    val name: String,
    @StringRes val use: Int,
    @StringRes val license: Int,
    val url: String,
    val licenseSection: String?,
    val termsLinks: List<NoticeLink> = emptyList(),
)

private const val APACHE_SECTION = "Apache License 2.0"

// Entry names and license section titles match the headings in both notice files.
internal val thirdPartyProjects = listOf(
    ThirdPartyProject(
        "Backdrop",
        R.string.notice_backdrop_use,
        R.string.notice_apache,
        "https://github.com/Kyant0/AndroidLiquidGlass",
        APACHE_SECTION,
    ),
    ThirdPartyProject(
        "Glasmio",
        R.string.notice_glasmio_use,
        R.string.notice_apache,
        "https://github.com/NEORUAA/Glasmio",
        APACHE_SECTION,
    ),
    ThirdPartyProject(
        "Google ML Kit Text Recognition v2",
        R.string.notice_ml_kit_use,
        R.string.notice_ml_kit_terms,
        "https://developers.google.com/ml-kit/vision/text-recognition/v2",
        licenseSection = null,
        termsLinks = listOf(
            NoticeLink(R.string.notice_ml_kit_terms, "https://developers.google.com/ml-kit/terms"),
            NoticeLink(R.string.notice_google_apis_terms, "https://developers.google.com/terms"),
        ),
    ),
    ThirdPartyProject(
        "Calendar (kizitonwose)",
        R.string.notice_calendar_use,
        R.string.notice_mit,
        "https://github.com/kizitonwose/Calendar",
        "MIT License (Calendar)",
    ),
    ThirdPartyProject(
        "Coil",
        R.string.notice_coil_use,
        R.string.notice_apache,
        "https://github.com/coil-kt/coil",
        APACHE_SECTION,
    ),
    ThirdPartyProject(
        "AndroidX",
        R.string.notice_androidx_use,
        R.string.notice_apache,
        "https://developer.android.com/jetpack/androidx",
        APACHE_SECTION,
    ),
    ThirdPartyProject(
        "Kotlin",
        R.string.notice_kotlin_use,
        R.string.notice_apache,
        "https://kotlinlang.org",
        APACHE_SECTION,
    ),
    ThirdPartyProject(
        "kotlinx.coroutines",
        R.string.notice_coroutines_use,
        R.string.notice_apache,
        "https://github.com/Kotlin/kotlinx.coroutines",
        APACHE_SECTION,
    ),
    ThirdPartyProject(
        "org.json",
        R.string.notice_json_use,
        R.string.notice_public_domain,
        "https://github.com/stleary/JSON-java",
        "JSON-java 20240303 License",
    ),
    ThirdPartyProject(
        "free-exercise-db",
        R.string.notice_free_exercise_db_use,
        R.string.notice_public_domain,
        "https://github.com/yuhonas/free-exercise-db",
        "Unlicense (free-exercise-db)",
    ),
)

internal fun readNoticeSections(text: String): Map<String, String> =
    text.replace("\r\n", "\n").split("\n## ").drop(1).associate { section ->
        section.substringBefore('\n') to section.substringAfter('\n').trim('\n')
    }

internal fun formatNoticeLicense(text: String): String =
    text.split("\n\n").mapIndexed { index, paragraph ->
        // Keep the license header on separate lines and let the UI wrap each prose paragraph.
        paragraph.lineSequence().joinToString(if (index == 0) "\n" else " ") { it.trim() }
    }.joinToString("\n\n")
