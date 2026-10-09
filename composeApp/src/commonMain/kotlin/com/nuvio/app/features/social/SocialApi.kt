package com.nuvio.app.features.social

import com.nuvio.app.core.network.SupabaseProvider
import com.nuvio.app.core.network.createApiHttpClient
import com.nuvio.app.core.network.readBoundedResponseBody
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpMethod
import io.ktor.http.contentLength
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Where the Nuvio Pro social server lives (Cloudflare Worker, source in servers/social), from
 * `NUVIO_SOCIAL_URL` in local.properties. Blank in builds without it: social is then off.
 */
internal val SocialServerUrl: String = SocialConfig.URL.trim().trimEnd('/')

/** A failed social call; [code] is the server's error code, e.g. "code_not_found". */
class SocialException(val status: Int, val code: String) : Exception("social $status $code")

internal val socialJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * Talks to the social server as the signed-in Nuvio account and the given profile. The account
 * is proven with the current Nuvio access token on every call; the server never stores it.
 */
internal class SocialApi(
    private val client: HttpClient = createApiHttpClient(),
) {
    suspend fun me(profile: Int): SocialMe = get("/v1/me", profile)

    suspend fun updateMe(profile: Int, name: String?, avatar: String?, sharing: Int?): SocialMe =
        send(HttpMethod.Put, "/v1/me", profile, buildJsonObject {
            name?.let { put("name", it) }
            if (avatar != null) put("avatar", avatar)
            sharing?.let { put("sharing", it) }
        })

    suspend fun friends(profile: Int): SocialFriends = get("/v1/friends", profile)

    suspend fun addFriend(profile: Int, code: String): SocialAddFriendResult =
        send(HttpMethod.Post, "/v1/friends/add", profile, buildJsonObject { put("code", code) })

    suspend fun acceptFriend(profile: Int, personId: String) =
        send<JsonElement>(HttpMethod.Post, "/v1/friends/accept", profile, personBody(personId))

    suspend fun declineFriend(profile: Int, personId: String) =
        send<JsonElement>(HttpMethod.Post, "/v1/friends/decline", profile, personBody(personId))

    suspend fun cancelRequest(profile: Int, personId: String) =
        send<JsonElement>(HttpMethod.Post, "/v1/friends/cancel", profile, personBody(personId))

    suspend fun removeFriend(profile: Int, personId: String) =
        send<JsonElement>(HttpMethod.Post, "/v1/friends/remove", profile, personBody(personId))

    suspend fun recommendations(profile: Int): SocialRecommendations = get("/v1/recommendations", profile)

    suspend fun recommend(
        profile: Int,
        to: List<String>,
        contentType: String,
        contentId: String,
        title: String,
        poster: String?,
        note: String?,
    ) = send<JsonElement>(HttpMethod.Post, "/v1/recommendations", profile, buildJsonObject {
        putJsonArray("to") { to.forEach(::add) }
        put("contentType", contentType)
        put("contentId", contentId)
        put("title", title)
        poster?.let { put("poster", it) }
        note?.let { put("note", it) }
    })

    suspend fun respond(profile: Int, id: String, reaction: String?, reply: String?) =
        send<JsonElement>(HttpMethod.Post, "/v1/recommendations/$id/respond", profile, buildJsonObject {
            reaction?.let { put("reaction", it) }
            reply?.let { put("reply", it) }
        })

    suspend fun markSeen(profile: Int, id: String) =
        send<JsonElement>(HttpMethod.Post, "/v1/recommendations/$id/seen", profile, JsonObject(emptyMap()))

    suspend fun recordActivity(profile: Int, body: JsonObject) =
        send<JsonElement>(HttpMethod.Post, "/v1/activity", profile, body)

    suspend fun feed(profile: Int): SocialFeed = get("/v1/feed", profile)

    suspend fun notifications(profile: Int): SocialNotifications = get("/v1/notifications", profile)

    private suspend inline fun <reified T> get(path: String, profile: Int): T =
        socialJson.decodeFromString(execute(HttpMethod.Get, path, profile, null))

    private suspend inline fun <reified T> send(method: HttpMethod, path: String, profile: Int, body: JsonObject): T =
        socialJson.decodeFromString(execute(method, path, profile, body.toString()))

    private suspend fun execute(method: HttpMethod, path: String, profile: Int, body: String?): String {
        val token = SupabaseProvider.client.auth.currentAccessTokenOrNull()
            ?: throw SocialException(401, "signed_out")
        return client.prepareRequest(SocialServerUrl + path) {
            this.method = method
            header("Accept", "application/json")
            header("Authorization", "Bearer $token")
            header("X-Nuvio-Profile", profile.toString())
            if (body != null) {
                header("Content-Type", "application/json")
                setBody(body)
            }
        }.execute { response ->
            val text = readBoundedResponseBody(response.bodyAsChannel(), response.contentLength())
            if (response.status.value !in 200..299) {
                val code = runCatching { socialJson.decodeFromString<SocialError>(text).error }.getOrNull()
                throw SocialException(response.status.value, code?.takeIf { it.isNotBlank() } ?: "http_${response.status.value}")
            }
            text
        }
    }

    private fun personBody(personId: String) = buildJsonObject { put("personId", JsonPrimitive(personId)) }
}
