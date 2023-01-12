package net.robinfriedli.filebroker

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.jvm.JvmStatic
import kotlin.time.Duration.Companion.seconds

class Api(var loginChangeCallback: ((Login?) -> Unit)? = null) {
    companion object {
        @JvmStatic
        val BASE_URL: String = "https://filebroker.io/api/"
    }

    private val http = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    var currentLogin: Login? = null
        set(value) {
            loginChangeCallback?.invoke(value)
            field = value
        }

    val loginRefreshLock = Mutex()

    @Serializable
    class Login(val token: String, val refreshToken: String, val expiry: Instant, val user: User)

    @Serializable
    class User(
        val user_name: String,
        val email: String?,
        val avatar_url: String?,
        val creation_timestamp: String
    )

    @Serializable
    class LoginRequest(val user_name: String, val password: String)

    @Serializable
    class LoginResponse(
        val token: String,
        val refresh_token: String,
        val expiration_secs: Int,
        val user: User
    )

    @Serializable
    class PostQueryObject(
        val pk: Int,
        val data_url: String?,
        val source_url: String?,
        val title: String?,
        val creation_timestamp: String,
        val fk_create_user: Int,
        val score: Int,
        val s3_object: String?,
        val thumbnail_url: String?,
        val thumbnail_object_key: String?,
        val is_public: Boolean,
        val public_edit: Boolean,
        val description: String?
    )

    @Serializable
    class SearchResult(val full_count: Long?, val pages: Long?, val posts: List<PostQueryObject>)

    @Serializable
    class PostDetailed(
        val pk: Int,
        val data_url: String?,
        val source_url: String?,
        val title: String?,
        val creation_timestamp: String,
        val fk_create_user: Int,
        val score: Int,
        val s3_object: S3Object?,
        val thumbnail_url: String?,
        val prev_post_pk: Int?,
        val next_post_pk: Int?,
        val is_public: Boolean,
        val public_edit: Boolean,
        val description: String?,
        val is_editable: Boolean,
        val tags: List<Tag>,
        val group_access: List<PostGroupAccessDetailed>
    )

    @Serializable
    class S3Object(
        val object_key: String,
        val sha256_hash: String?,
        val size_bytes: Long,
        val mime_type: String,
        val fk_broker: Int,
        val fk_uploader: Int,
        val thumbnail_object_key: String?,
        val creation_timestamp: String,
        val filename: String?
    )

    @Serializable
    class Tag(
        val pk: Int,
        val tag_name: String,
        val creation_timestamp: String
    )

    @Serializable
    class PostGroupAccessDetailed(
        val fk_post: Int,
        val write: Boolean,
        val fk_granted_by: Int,
        val creation_timestamp: String,
        val granted_group: UserGroup
    )

    @Serializable
    class UserGroup(
        val pk: Int,
        val name: String,
        val is_public: Boolean,
        val hidden: Boolean,
        val fk_owner: Int,
        val creation_timestamp: String
    )

    @Throws(Exception::class)
    suspend fun login(request: LoginRequest): LoginResponse {
        val response = http.post(BASE_URL + "login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status.isSuccess()) {
            val loginResponse = response.body<LoginResponse>()
            handleLoginResponse(loginResponse)

            return loginResponse
        } else if (response.status.value == 401) {
            throw InvalidCredentialsException(response.bodyAsText())
        } else {
            throw InvalidHttpResponseException(response.status.value, response.bodyAsText())
        }
    }

    fun handleLoginResponse(loginResponse: LoginResponse): Login {
        val expirationSecs = loginResponse.expiration_secs / 3 * 2
        val now = Clock.System.now()
        val login = Login(
            loginResponse.token,
            loginResponse.refresh_token,
            now.plus(expirationSecs.seconds),
            loginResponse.user
        )
        currentLogin =
            login
        return login
    }

    suspend fun getCurrentLogin(): Login? {
        val currentLogin = this.currentLogin
        return if (currentLogin != null && currentLogin.expiry < Clock.System.now()) {
            loginRefreshLock.withLock {
                // recheck after acquiring lock
                val currentLogin = this.currentLogin
                if (currentLogin != null && currentLogin.expiry < Clock.System.now()) {
                    val response =
                        http.post(BASE_URL + "refresh-token/" + currentLogin.refreshToken)

                    if (response.status.isSuccess()) {
                        try {
                            handleLoginResponse(response.body())
                        } catch (e: Exception) {
                            Napier.e("Failed to refresh login with exception", e)
                            currentLogin
                        }
                    } else if (response.status.value == 401) {
                        Napier.w("Failed to refresh login with status 401")
                        this.currentLogin = null
                        null
                    } else {
                        Napier.e("Failed to refresh login with status ${response.status.value}")
                        currentLogin
                    }
                } else {
                    currentLogin
                }
            }
        } else {
            currentLogin
        }
    }

    suspend fun refreshLogin(refreshToken: String): Login? {
        return loginRefreshLock.withLock {
            val response =
                http.post(BASE_URL + "refresh-token/" + refreshToken)

            if (response.status.isSuccess()) {
                try {
                    handleLoginResponse(response.body())
                } catch (e: Exception) {
                    Napier.e("Failed to refresh login with exception", e)
                    null
                }
            } else {
                Napier.e("Failed to refresh login with status ${response.status.value}")
                null
            }
        }
    }

    @Throws(Exception::class)
    suspend fun search(query: String? = null, page: Long = 0): SearchResult {
        val currentLogin = getCurrentLogin()
        val response = http.get(BASE_URL + "search") {
            if (query != null) {
                parameter("query", query)
            }
            parameter("page", page)
            if (currentLogin != null) {
                header("Authorization", "Bearer " + currentLogin.token)
            }
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw InvalidHttpResponseException(response.status.value, response.bodyAsText())
        }
    }

    @Throws(Exception::class)
    suspend fun getPost(key: Int, query: String? = null, page: Long = 0): PostDetailed {
        val currentLogin = getCurrentLogin()
        val response = http.get(BASE_URL + "get-post/" + key) {
            if (query != null) {
                parameter("query", query)
            }
            parameter("page", page)
            if (currentLogin != null) {
                header("Authorization", "Bearer " + currentLogin.token)
            }
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw InvalidHttpResponseException(response.status.value, response.bodyAsText())
        }
    }

    open class InvalidHttpResponseException(val status: Int, val body: String) :
        RuntimeException("Received invalid status code $status, see response body for details")

    class InvalidCredentialsException(body: String) : InvalidHttpResponseException(401, body)
}
