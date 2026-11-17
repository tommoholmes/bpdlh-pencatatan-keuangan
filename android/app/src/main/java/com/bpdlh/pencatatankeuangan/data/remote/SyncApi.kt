package com.bpdlh.pencatatankeuangan.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(val status: Int, message: String) : IOException(message)

/**
 * Satu-satunya jalur ke Worker. Setiap request membawa ID token Firebase terbaru;
 * `uid` tidak pernah dikirim dari klien.
 */
class SyncApi(
    private val baseUrl: String,
    private val tokenProvider: suspend () -> String?,
    private val client: OkHttpClient = defaultClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun bootstrap(syncId: String? = null): String {
        val response = send(Request.Builder().url("$baseUrl/v1/me/bootstrap").post(EMPTY).build(), syncId)
        return json.decodeFromString<BootstrapResponse>(response).logId
    }

    suspend fun push(deviceId: String, records: List<PushRecord>, syncId: String? = null): PushResponse {
        val body = json.encodeToString(PushBody(deviceId, records)).toRequestBody(JSON)
        val response = send(Request.Builder().url("$baseUrl/v1/sync/push").post(body).build(), syncId)
        return json.decodeFromString(response)
    }

    suspend fun pull(since: Long, limit: Int = 200, syncId: String? = null): PullResponse {
        val request = Request.Builder().url("$baseUrl/v1/sync/pull?since=$since&limit=$limit").get().build()
        return json.decodeFromString(send(request, syncId))
    }

    suspend fun putPhoto(photoId: String, bytes: ByteArray, syncId: String? = null) {
        val request = Request.Builder()
            .url("$baseUrl/v1/photos/$photoId")
            .put(bytes.toRequestBody(JPEG))
            .build()
        send(request, syncId)
    }

    suspend fun getPhoto(photoId: String): ByteArray {
        val request = Request.Builder().url("$baseUrl/v1/photos/$photoId").get().build()
        return withContext(Dispatchers.IO) {
            client.newCall(request.withToken(null)).execute().use { response ->
                if (!response.isSuccessful) throw ApiException(response.code, "GET foto gagal: ${response.code}")
                response.body.bytes()
            }
        }
    }

    suspend fun deleteAccount() {
        send(Request.Builder().url("$baseUrl/v1/me").delete().build(), null)
    }

    private suspend fun send(request: Request, syncId: String?): String = withContext(Dispatchers.IO) {
        client.newCall(request.withToken(syncId)).execute().use { response ->
            val text = response.body.string()
            if (!response.isSuccessful) {
                throw ApiException(response.code, "HTTP ${response.code}: ${text.take(200)}")
            }
            text
        }
    }

    private suspend fun Request.withToken(syncId: String?): Request {
        val token = tokenProvider() ?: throw ApiException(401, "Belum masuk")
        return newBuilder()
            .header("Authorization", "Bearer $token")
            .apply { if (syncId != null) header("X-Sync-Id", syncId) }
            .build()
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        val JPEG = "image/jpeg".toMediaType()
        val EMPTY = ByteArray(0).toRequestBody(JSON)

        fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
