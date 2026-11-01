package com.sonnik.topower.domain

/** Выпуск на GitHub: версия, ссылки и что нового. */
data class ReleaseInfo(
    val version: String,
    val pageUrl: String,
    val apkUrl: String?,
    val notes: String,
    /** Время публикации, epoch millis. */
    val publishedAt: Long? = null,
    /** Размер APK, байт. */
    val apkBytes: Long? = null,
) {
    /** «v1.27» → «1.27». */
    val label: String get() = version.removePrefix("v").removePrefix("V")
}

/** Проверка обновлений — чистая часть: сравнение версий и когда пора проверять снова (как в Pin). */
object Updates {
    /** Последний выпуск SonnikToPower — без ключей и входа, поэтому репозиторий должен быть публичным. */
    const val LATEST_URL = "https://api.github.com/repos/Sonnik73/SonnikToPoer/releases/latest"
    const val RELEASES_PAGE = "https://github.com/Sonnik73/SonnikToPoer/releases/latest"

    /** Не чаще раза в 6 часов — при каждом входе в приложение GitHub не спрашиваем. */
    const val INTERVAL_MS = 6 * 60 * 60 * 1000L

    /** «v1.27» → [1, 27]; «1.2.3-beta» → [1, 2, 3]. Непонятное — пустой список. */
    fun parts(version: String): List<Int> =
        version.trim().removePrefix("v").removePrefix("V").substringBefore('-').split('.')
            .map { it.takeWhile(Char::isDigit) }
            .takeIf { list -> list.isNotEmpty() && list.all { it.isNotEmpty() } }
            ?.map { it.toInt() }
            .orEmpty()

    /** Новее ли [candidate], чем [current]. «1.10» новее «1.9»: сравниваются числа, а не строки. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parts(candidate)
        val b = parts(current)
        if (a.isEmpty() || b.isEmpty()) return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    fun isDue(lastCheck: Long, now: Long): Boolean = now - lastCheck >= INTERVAL_MS || now < lastCheck

    /** Предлагать ли выпуск: новее текущей версии и не тот, что пользователь пропустил. */
    fun shouldOffer(release: ReleaseInfo?, current: String, skipped: String?): Boolean =
        release != null && isNewer(release.version, current) && parts(release.version) != skipped?.let(::parts)

    /** Текст выпуска без первого заголовка «## 1.27» — версия и так в заголовке окна. */
    fun cleanNotes(body: String): String =
        body.lines().dropWhile { it.isBlank() || it.trimStart().startsWith("#") }.joinToString("\n").trim()

    /** Размер в мегабайтах с одним знаком: 7 586 445 → «7.2». */
    fun megabytes(bytes: Long): String = "%.1f".format(java.util.Locale.ROOT, bytes / 1024.0 / 1024.0)
}
