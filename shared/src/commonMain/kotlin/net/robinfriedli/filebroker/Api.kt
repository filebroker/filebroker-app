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
import kotlin.jvm.JvmStatic
import kotlin.time.Duration.Companion.seconds

class Api {
    companion object {
        @JvmStatic
        val BASE_URL: String = "https://filebroker.io/api/"
    }

    private val http = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    var currentLogin: Login? = null

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

    suspend fun search(query: String?, page: Long = 0): SearchResult {
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

    open class InvalidHttpResponseException(val status: Int, val body: String) :
        RuntimeException("Received invalid status code $status, see response body for details")

    class InvalidCredentialsException(body: String) : InvalidHttpResponseException(401, body)
}
