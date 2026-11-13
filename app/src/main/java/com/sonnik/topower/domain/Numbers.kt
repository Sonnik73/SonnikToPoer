package com.sonnik.topower.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** Разбирает число, допуская и точку, и запятую. Пустая строка — null. */
fun parseDecimal(text: String): Double? =
    text.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()

fun parseInt(text: String): Int? = text.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

/**
 * Поле с числом заполнено правильно: пустое — можно, иначе число, которое разбирается
 * ([parseDecimal] или [parseInt]) и не отрицательное, если минус не разрешён.
 */
fun isValidNumber(text: String, decimal: Boolean = true, allowNegative: Boolean = false): Boolean {
    if (text.isBlank()) return true
    val value = if (decimal) parseDecimal(text) else parseInt(text)?.toDouble()
    return value != null && value.isFinite() && (allowNegative || value >= 0)
}

/** 60.0 -> "60", 62.5 -> "62.5", 1.333 -> "1.33". */
fun formatNumber(value: Double, maxDecimals: Int = 2): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    return BigDecimal.valueOf(value)
        .setScale(maxDecimals, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
        .let { if (it == "-0") "0" else it }
}

/**
 * Большие числа для плиток статистики: целая часть разбивается на группы по 3 цифры
 * узким неразрывным пробелом — «41780» → «41 780». Дробная часть (по [maxDecimals]) не группируется.
 */
fun formatBig(value: Double, maxDecimals: Int = 0): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    val text = formatNumber(value, maxDecimals)
    val dot = text.indexOf('.')
    val (integer, fraction) = if (dot >= 0) text.substring(0, dot) to text.substring(dot) else text to ""
    val sign = if (integer.startsWith("-")) "-" else ""
    val digits = integer.removePrefix("-")
    val grouped = digits.reversed().chunked(3).joinToString("\u202F").reversed()
    return sign + grouped + fraction
}

/** Секунды в "m:ss" или "h:mm:ss". */
fun formatDuration(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
