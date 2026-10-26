package com.sonnik.ecosystem.contract

/**
 * Контракт обмена данными SonnikToPower с приложениями экосистемы «Гарнизон».
 * Файл самодостаточен: без зависимостей от остального кода, копируется в приложения-читатели как есть.
 *
 * Данные только на чтение: ContentProvider [AUTHORITY], доступ по разрешению [PERMISSION_READ]
 * (protectionLevel="signature" — только приложения, подписанные тем же ключом).
 * Даты — epoch millis (Long), веса — кг (Double), мышцы — имена enum Muscle (CHEST, LATS…).
 *
 * Версии: колонки и пути можно добавлять без смены [SCHEMA_VERSION]; удаление или переименование —
 * только с увеличением [SCHEMA_VERSION] и сохранением старого варианта ещё на одну версию.
 * Полное описание — docs/ecosystem/CONTRACT.md.
 */
object EcosystemContract {
    const val SCHEMA_VERSION = 1

    const val AUTHORITY = "com.sonnik.topower.ecosystem"
    const val PERMISSION_READ = "com.sonnik.ecosystem.permission.READ_DATA"

    /** Базовый URI: на него приходит notifyChange при любом изменении данных тренировок. */
    const val BASE_URI = "content://$AUTHORITY/"

    /** Query-параметры периода, epoch millis. Без них — последние [DEFAULT_PERIOD_DAYS] дней. */
    const val PARAM_FROM = "from"
    const val PARAM_TO = "to"
    const val PARAM_SINCE = "since"
    const val DEFAULT_PERIOD_DAYS = 7

    object Info {
        const val PATH = "info"
        const val URI = BASE_URI + PATH

        const val SCHEMA_VERSION = "schema_version"
        const val APP_VERSION = "app_version"
        const val LAST_CHANGED_AT = "last_changed_at"

        @JvmField val COLUMNS = arrayOf(SCHEMA_VERSION, APP_VERSION, LAST_CHANGED_AT)
    }

    object Workouts {
        const val PATH = "workouts"
        const val URI = BASE_URI + PATH

        const val ID = "id"
        const val STARTED_AT = "started_at"
        const val FINISHED_AT = "finished_at"
        const val DURATION_SEC = "duration_sec"
        const val WORKING_SETS = "working_sets"
        const val TONNAGE_KG = "tonnage_kg"
        const val BODY_WEIGHT_KG = "body_weight_kg"
        const val AVG_HR = "avg_hr"
        const val MAX_HR = "max_hr"
        const val CALORIES = "calories"

        @JvmField val COLUMNS = arrayOf(
            ID, STARTED_AT, FINISHED_AT, DURATION_SEC, WORKING_SETS, TONNAGE_KG, BODY_WEIGHT_KG, AVG_HR, MAX_HR, CALORIES,
        )
    }

    object MuscleVolume {
        const val PATH = "muscle_volume"
        const val URI = BASE_URI + PATH

        const val MUSCLE = "muscle"
        const val WORKING_SETS = "working_sets"
        const val REPS = "reps"
        const val TONNAGE_KG = "tonnage_kg"
        const val WORKOUTS = "workouts"

        @JvmField val COLUMNS = arrayOf(MUSCLE, WORKING_SETS, REPS, TONNAGE_KG, WORKOUTS)
    }

    object Recovery {
        const val PATH = "recovery"
        const val URI = BASE_URI + PATH

        const val MUSCLE = "muscle"
        const val STATE = "state"
        const val LAST_TRAINED_AT = "last_trained_at"
        const val READY_AT = "ready_at"
        const val RECOVERED_FRACTION = "recovered_fraction"

        const val STATE_RECOVERING = "RECOVERING"
        const val STATE_READY = "READY"
        const val STATE_STALE = "STALE"
        const val STATE_NEVER = "NEVER"

        @JvmField val COLUMNS = arrayOf(MUSCLE, STATE, LAST_TRAINED_AT, READY_AT, RECOVERED_FRACTION)
    }

    object BodyWeight {
        const val PATH = "body_weight"
        const val URI = BASE_URI + PATH

        const val MEASURED_AT = "measured_at"
        const val WEIGHT_KG = "weight_kg"

        @JvmField val COLUMNS = arrayOf(MEASURED_AT, WEIGHT_KG)
    }

    object Records {
        const val PATH = "records"
        const val URI = BASE_URI + PATH

        const val EXERCISE_ID = "exercise_id"
        const val EXERCISE_NAME = "exercise_name"
        const val METRIC = "metric"
        const val VALUE = "value"
        const val ACHIEVED_AT = "achieved_at"

        const val METRIC_WEIGHT = "WEIGHT"
        const val METRIC_REPS = "REPS"
        const val METRIC_TIME = "TIME"
        const val METRIC_DISTANCE = "DISTANCE"
        const val METRIC_E1RM = "E1RM"

        @JvmField val COLUMNS = arrayOf(EXERCISE_ID, EXERCISE_NAME, METRIC, VALUE, ACHIEVED_AT)
    }
}
