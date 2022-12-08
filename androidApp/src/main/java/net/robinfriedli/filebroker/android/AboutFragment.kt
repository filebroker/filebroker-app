package net.robinfriedli.filebroker.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import net.robinfriedli.filebroker.getPlatform

class AboutFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)

        view.findViewById<TextView>(R.id.filebrokerDescription).text =
            resources.getString(R.string.filebroker_description, "0.1")

        view.findViewById<TextView>(R.id.filebrokerPlatform).text =
            resources.getString(R.string.filebroker_platform, getPlatform().name)

        return view
    }
}