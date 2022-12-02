package net.robinfriedli.filebroker

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
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

    suspend fun login(request: LoginRequest): LoginResponse {
        val response = http.post(BASE_URL + "login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status.isSuccess()) {
            val loginResponse = response.body<LoginResponse>()
            val expirationSecs = loginResponse.expiration_secs / 3 * 2
            val now = Clock.System.now()
            currentLogin =
                Login(
                    loginResponse.token,
                    loginResponse.refresh_token,
                    now.plus(expirationSecs.seconds),
                    loginResponse.user
                )

            return loginResponse
        } else if (response.status.value == 401) {
            throw InvalidCredentialsException(response.bodyAsText())
        } else {
            throw InvalidHttpResponseException(response.status.value, response.bodyAsText())
        }
    }

    open class InvalidHttpResponseException(val status: Int, val body: String) :
        RuntimeException("Received invalid status code $status, see response body for details")

    class InvalidCredentialsException(body: String) : InvalidHttpResponseException(401, body)
}
