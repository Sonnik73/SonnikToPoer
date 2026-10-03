package com.sonnik.topower.ui.changelog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChangelogTest {

    @Test
    fun parsesSections() {
        val entries = parseChangelog(
            """
            # Title

            ## 1.1
            - Second
            - Third

            ## 1.0
            - First
            """.trimIndent()
        )
        assertEquals(listOf("1.1", "1.0"), entries.map { it.version })
        assertEquals(listOf("Second", "Third"), entries[0].items)
    }

    /** Журналы на обоих языках описывают одни и те же версии, а первая из них — текущая версия приложения. */
    @Test
    fun changelogFilesMatchAppVersion() {
        val ru = parseChangelog(File("../changelog/changelog_ru.md").readText())
        val en = parseChangelog(File("../changelog/changelog_en.md").readText())
        assertEquals(ru.map { it.version }, en.map { it.version })
        assertTrue(ru.all { it.items.isNotEmpty() })
        val appVersion = File("../gradle.properties").readLines()
            .first { it.startsWith("appVersion=") }.substringAfter("=").trim()
        assertEquals(appVersion, ru.first().version)
    }

    /**
     * Для F-Droid у каждой версии есть changelogs/<код>.txt (код = 1.20 → 120) с теми же пунктами,
     * что и в журнале, не длиннее 500 символов.
     */
    @Test
    fun fastlaneChangelogMatchesCurrentVersion() {
        val appVersion = File("../gradle.properties").readLines()
            .first { it.startsWith("appVersion=") }.substringAfter("=").trim()
        val code = appVersion.split(".").let { (major, minor) -> major.toInt() * 100 + minor.toInt() }
        val declared = File("../gradle.properties").readLines()
            .first { it.startsWith("appVersionCode=") }.substringAfter("=").trim().toInt()
        assertEquals("appVersionCode в gradle.properties", code, declared)
        listOf("ru-RU" to "changelog_ru.md", "en-US" to "changelog_en.md").forEach { (locale, journal) ->
            val file = File("../fastlane/metadata/android/$locale/changelogs/$code.txt")
            assertTrue("нет ${file.path}", file.exists())
            val text = file.readText()
            assertTrue("${file.path}: ${text.length} > 500", text.length <= 500)
            val items = parseChangelog(File("../changelog/$journal").readText()).first().items
            assertEquals(items, text.lines().filter { it.isNotBlank() }.map { it.removePrefix("• ") })
        }
    }

    @Test
    fun shortDescriptionsFit() {
        listOf("ru-RU", "en-US").forEach { locale ->
            val text = File("../fastlane/metadata/android/$locale/short_description.txt").readText()
            assertTrue("$locale: ${text.length}", text.isNotBlank() && text.length <= 80)
        }
    }
}
