package com.sonnik.topower.ecosystem

import android.content.Context
import android.net.Uri
import com.sonnik.ecosystem.contract.EcosystemContract

/**
 * Отметка последнего изменения данных тренировок и оповещение подписчиков провайдера.
 * Отметка нужна, потому что удаление тренировки или подхода не оставляет следа в updatedAt.
 */
object EcosystemChanges {
    private const val PREFS = "ecosystem"
    private const val KEY_LAST_CHANGED = "last_changed_at"

    fun lastChangedAt(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_CHANGED, 0L)

    /** Запоминает время изменения и вызывает notifyChange для [EcosystemContract.BASE_URI]. */
    fun onDataChanged(context: Context, now: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_LAST_CHANGED, now).apply()
        context.contentResolver.notifyChange(Uri.parse(EcosystemContract.BASE_URI), null)
    }
}
