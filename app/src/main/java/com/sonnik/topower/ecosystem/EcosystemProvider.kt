package com.sonnik.topower.ecosystem

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.sonnik.ecosystem.contract.EcosystemContract
import com.sonnik.topower.SonnikApp
import com.sonnik.topower.data.AppDao
import kotlinx.coroutines.runBlocking

/**
 * Сводки тренировок для приложений экосистемы — только чтение, только по разрешению
 * [EcosystemContract.PERMISSION_READ] (подпись тем же ключом). Внутренняя схема базы наружу не видна:
 * каждая выдача собирается в [EcosystemData] из существующих расчётов статистики.
 */
class EcosystemProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val context = requireNotNull(context)
        val now = System.currentTimeMillis()
        val data = EcosystemData(dao ?: (context.applicationContext as SonnikApp).repository.dao)
        // Провайдер вызывается из потоков Binder, а не из главного: ждать базу здесь можно.
        val (columns, rows) = runBlocking {
            when (MATCHER.match(uri)) {
                INFO -> EcosystemContract.Info.COLUMNS to data.info(appVersion(context), EcosystemChanges.lastChangedAt(context))
                WORKOUTS -> EcosystemContract.Workouts.COLUMNS to data.workouts(range(uri, now))
                MUSCLE_VOLUME -> EcosystemContract.MuscleVolume.COLUMNS to data.muscleVolume(range(uri, now))
                RECOVERY -> EcosystemContract.Recovery.COLUMNS to data.recovery(now)
                BODY_WEIGHT -> EcosystemContract.BodyWeight.COLUMNS to data.bodyWeight(range(uri, now))
                RECORDS -> EcosystemContract.Records.COLUMNS to
                    data.records(longParam(uri, EcosystemContract.PARAM_SINCE) ?: TimeRange.lastDays(now).from)
                else -> throw IllegalArgumentException("Unknown URI: $uri")
            }
        }
        return cursor(columns, rows, projection).apply {
            setNotificationUri(context.contentResolver, Uri.parse(EcosystemContract.BASE_URI))
        }
    }

    override fun getType(uri: Uri): String? {
        val path = PATHS[MATCHER.match(uri)] ?: return null
        val kind = if (path == EcosystemContract.Info.PATH) "vnd.android.cursor.item" else "vnd.android.cursor.dir"
        return "$kind/vnd.${EcosystemContract.AUTHORITY}.$path"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri = throw UnsupportedOperationException(READ_ONLY)

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException(READ_ONLY)

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException(READ_ONLY)

    override fun bulkInsert(uri: Uri, values: Array<out ContentValues>): Int = throw UnsupportedOperationException(READ_ONLY)

    companion object {
        private const val READ_ONLY = "SonnikToPower ecosystem data is read-only"

        private const val INFO = 1
        private const val WORKOUTS = 2
        private const val MUSCLE_VOLUME = 3
        private const val RECOVERY = 4
        private const val BODY_WEIGHT = 5
        private const val RECORDS = 6

        private val PATHS = mapOf(
            INFO to EcosystemContract.Info.PATH,
            WORKOUTS to EcosystemContract.Workouts.PATH,
            MUSCLE_VOLUME to EcosystemContract.MuscleVolume.PATH,
            RECOVERY to EcosystemContract.Recovery.PATH,
            BODY_WEIGHT to EcosystemContract.BodyWeight.PATH,
            RECORDS to EcosystemContract.Records.PATH,
        )

        private val MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            PATHS.forEach { (code, path) -> addURI(EcosystemContract.AUTHORITY, path, code) }
        }

        /** База для тестов; в приложении — база [SonnikApp]. */
        @VisibleForTesting
        internal var dao: AppDao? = null

        private fun appVersion(context: Context): String =
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

        private fun longParam(uri: Uri, name: String): Long? = uri.getQueryParameter(name)?.let {
            it.toLongOrNull() ?: throw IllegalArgumentException("$name must be epoch millis: $it")
        }

        private fun range(uri: Uri, now: Long): TimeRange {
            val default = TimeRange.lastDays(now)
            return TimeRange(
                from = longParam(uri, EcosystemContract.PARAM_FROM) ?: default.from,
                to = longParam(uri, EcosystemContract.PARAM_TO) ?: default.to,
            )
        }

        /** Курсор с запрошенными колонками ([projection]) в указанном порядке или со всеми. */
        private fun cursor(columns: Array<String>, rows: List<Array<Any?>>, projection: Array<out String>?): MatrixCursor {
            val picked = projection?.map { name ->
                columns.indexOf(name).also { require(it >= 0) { "Unknown column: $name" } }
            } ?: columns.indices.toList()
            return MatrixCursor(picked.map { columns[it] }.toTypedArray(), rows.size).apply {
                rows.forEach { row -> addRow(picked.map { row[it] }) }
            }
        }
    }
}
