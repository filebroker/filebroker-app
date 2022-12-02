package net.robinfriedli.filebroker.android

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import kotlinx.coroutines.runBlocking
import net.robinfriedli.filebroker.Api

class LoginFragment(val api: Api) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        val userInput = view.findViewById<EditText>(R.id.userInput)
        val passwordInput = view.findViewById<EditText>(R.id.passwordInput)
        val loginButton = view.findViewById<Button>(R.id.loginButton)
        loginButton.isEnabled = false

        userInput.doOnTextChanged { text, start, before, count ->
            loginButton.isEnabled = !text.isNullOrEmpty() && !passwordInput.text.isNullOrEmpty()
        }

        passwordInput.doOnTextChanged { text, start, before, count ->
            loginButton.isEnabled = !text.isNullOrEmpty() && !userInput.text.isNullOrEmpty()
        }

        loginButton.setOnClickListener { buttonView ->
            try {
                userInput.hideKeyboard()
                passwordInput.hideKeyboard()
                runBlocking {
                    api.login(
                        Api.LoginRequest(
                            userInput.text.toString(),
                            passwordInput.text.toString()
                        )
                    )

                    (activity as MainActivity).onLoginCompleted(HomeFragment())
                }
            } catch (e: Api.InvalidCredentialsException) {
                passwordInput.setError("Invalid credentials")
            } catch (e: Exception) {
                Log.e(this.javaClass.simpleName, "Error logging in", e)
                val builder = AlertDialog.Builder(this.context)
                builder.setTitle("Error")
                builder.setMessage("Login failed")
                builder.setPositiveButton(R.string.ok) { dialogInterface, _ -> dialogInterface.dismiss() }
                builder.show()
            }
        }

        return view
    }
}