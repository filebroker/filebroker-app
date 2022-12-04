package net.robinfriedli.filebroker.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ProfileFragment() : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val currentLogin = (activity as MainActivity).api.currentLogin
        view.findViewById<TextView>(R.id.profileName).text =
            currentLogin?.user?.user_name ?: "Not Logged In"
        return view
    }
}