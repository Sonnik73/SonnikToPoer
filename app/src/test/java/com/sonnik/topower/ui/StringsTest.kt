package com.sonnik.topower.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Строковые ресурсы: одинаковые ключи на обоих языках и экранированные апострофы (иначе сборка падает). */
class StringsTest {
    private val stringRegex = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""")

    private fun strings(dir: String) =
        stringRegex.findAll(File("src/main/res/$dir/strings.xml").readText()).associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun sameKeysInBothLanguages() {
        val en = strings("values").keys - "app_name"
        val ru = strings("values-ru").keys
        assertEquals(en.sorted(), ru.sorted())
    }

    @Test
    fun apostrophesAreEscaped() {
        listOf("values", "values-ru").forEach { dir ->
            strings(dir).forEach { (key, value) ->
                val quoted = value.startsWith("\"") && value.endsWith("\"")
                assertTrue("$dir/$key: unescaped apostrophe", quoted || !Regex("""(?<!\\)'""").containsMatchIn(value))
            }
        }
    }
}
