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
}
