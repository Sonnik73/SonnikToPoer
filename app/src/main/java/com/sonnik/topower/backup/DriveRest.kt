package com.sonnik.topower.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Google Drive REST v3 без сторонних библиотек, как в Pin. Область `drive.file`: приложение видит
 * только свои файлы, поэтому папку «SonnikToPower» и копии в ней создаёт само приложение.
 */
class DriveRest(private val token: String, private val signInAgain: String) : BackupStore {
    private var folderId: String? = null

    private suspend fun request(method: String, url: String, body: ByteArray? = null, contentType: String? = null): String =
        withContext(Dispatchers.IO) {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 20_000
                readTimeout = 60_000
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", contentType ?: "application/json; charset=UTF-8")
                }
            }
            try {
                body?.let { b -> conn.outputStream.use { it.write(b) } }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
                when {
                    code == 401 -> throw NeedsSignInException(signInAgain)
                    code == 403 && text.contains("insufficient", ignoreCase = true) -> throw NeedsSignInException(signInAgain)
                    code !in 200..299 -> throw IOException("Google Drive $code: ${text.take(300)}")
                }
                text
            } finally {
                conn.disconnect()
            }
        }

    private fun q(s: String) = URLEncoder.encode(s, "UTF-8")

    private suspend fun folder(): String {
        folderId?.let { return it }
        val query = "mimeType='$FOLDER_MIME' and name='$FOLDER' and 'root' in parents and trashed=false"
        val found = JSONObject(request("GET", "$API/files?q=${q(query)}&fields=files(id)&spaces=drive"))
            .optJSONArray("files")?.optJSONObject(0)?.optString("id")?.takeIf { it.isNotBlank() }
        val id = found ?: run {
            val meta = JSONObject().put("name", FOLDER).put("mimeType", FOLDER_MIME).put("parents", JSONArray().put("root"))
            JSONObject(request("POST", "$API/files?fields=id", meta.toString().toByteArray())).getString("id")
        }
        return id.also { folderId = it }
    }

    override suspend fun list(): List<RemoteBackup> {
        val parent = folder()
        val out = ArrayList<RemoteBackup>()
        var pageToken: String? = null
        do {
            val url = "$API/files?q=${q("'$parent' in parents and trashed=false and mimeType!='$FOLDER_MIME'")}" +
                "&fields=${q("nextPageToken,files($FIELDS)")}&orderBy=${q("modifiedTime desc")}&pageSize=100" +
                (pageToken?.let { "&pageToken=${q(it)}" } ?: "")
            val res = JSONObject(request("GET", url))
            val files = res.optJSONArray("files") ?: JSONArray()
            for (i in 0 until files.length()) out += parse(files.getJSONObject(i))
            pageToken = res.optString("nextPageToken").takeIf { it.isNotBlank() }
        } while (pageToken != null)
        return out.sortedByDescending { it.modified }
    }

    private fun parse(o: JSONObject) = RemoteBackup(
        id = o.getString("id"),
        name = o.getString("name"),
        modified = o.optString("modifiedTime").takeIf { it.isNotBlank() }
            ?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() } ?: 0L,
        bytes = o.optString("size").toLongOrNull(),
    )

    override suspend fun upload(name: String, json: String): RemoteBackup {
        val meta = JSONObject().put("name", name).put("mimeType", "application/json").put("parents", JSONArray().put(folder()))
        val boundary = "stp-" + UUID.randomUUID()
        val body = buildString {
            append("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n")
            append(meta.toString()).append("\r\n")
            append("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n")
            append(json).append("\r\n")
            append("--$boundary--\r\n")
        }.toByteArray(Charsets.UTF_8)
        val res = request("POST", "$UPLOAD/files?uploadType=multipart&fields=$FIELDS", body, "multipart/related; boundary=$boundary")
        return parse(JSONObject(res))
    }

    override suspend fun download(backup: RemoteBackup): String = request("GET", "$API/files/${backup.id}?alt=media")

    override suspend fun delete(backup: RemoteBackup) {
        request("DELETE", "$API/files/${backup.id}")
    }

    private companion object {
        const val API = "https://www.googleapis.com/drive/v3"
        const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        const val FOLDER = "SonnikToPower"
        const val FOLDER_MIME = "application/vnd.google-apps.folder"
        const val FIELDS = "id,name,modifiedTime,size"
    }
}
