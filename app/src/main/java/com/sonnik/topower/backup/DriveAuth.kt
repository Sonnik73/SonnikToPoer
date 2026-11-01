package com.sonnik.topower.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.sonnik.topower.R
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * Вход в Google Диск через Authorization API, как в Pin: только область `drive.file` —
 * приложение видит лишь созданные им файлы. OAuth-клиент Android в Google Cloud опознаёт
 * SonnikToPower по пакету и SHA-1 ключа подписи, секретов в приложении нет.
 */
object DriveAuth {
    const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"

    private val request: AuthorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(SCOPE_DRIVE_FILE))).build()

    /** Есть ли на телефоне сервисы Google Play — без них вход не работает. */
    fun playServicesAvailable(context: Context): Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    /** Запрос доступа с экрана; может потребоваться окно выбора аккаунта ([AuthorizationResult.hasResolution]). */
    fun authorize(activity: Activity): Task<AuthorizationResult> = Identity.getAuthorizationClient(activity).authorize(request)

    fun resultFromIntent(context: Context, data: Intent?): AuthorizationResult =
        Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)

    /** Токен в фоне (не в главном потоке). Если нужен экран входа — [NeedsSignInException]. */
    fun tokenBlocking(context: Context): String {
        val result = try {
            Tasks.await(Identity.getAuthorizationClient(context).authorize(request), 30, TimeUnit.SECONDS)
        } catch (e: ExecutionException) {
            throw NeedsSignInException(describe(context, e.cause ?: e))
        }
        if (result.hasResolution()) throw NeedsSignInException(context.getString(R.string.drive_sign_in_again))
        return result.accessToken ?: throw NeedsSignInException(context.getString(R.string.drive_sign_in_again))
    }

    /** Понятное описание ошибки входа. */
    fun describe(context: Context, e: Throwable): String = when {
        e.message?.contains("UNREGISTERED_ON_API_CONSOLE") == true ||
            (e as? ApiException)?.statusCode == 10 -> context.getString(R.string.drive_error_unregistered)
        (e as? ApiException)?.statusCode == 7 -> context.getString(R.string.drive_error_network)
        (e as? ApiException)?.statusCode == 16 -> context.getString(R.string.drive_error_cancelled)
        else -> e.message ?: e.javaClass.simpleName
    }
}

/** Нужен повторный вход в Google (доступ отозван или истёк). */
class NeedsSignInException(message: String) : Exception(message)
