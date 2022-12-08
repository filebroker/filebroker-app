package net.robinfriedli.filebroker.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val currentLogin = (activity as MainActivity).api.currentLogin
        view.findViewById<TextView>(R.id.profileName).text =
            resources.getString(R.string.welcome, currentLogin?.user?.user_name ?: "Not Logged In")

        view.findViewById<Button>(R.id.logoutButton).setOnClickListener {
            val mainActivity = activity as MainActivity
            mainActivity.api.currentLogin = null
            mainActivity.onLoginCompleted(R.id.homeFragment)
        }

        return view
    }
}