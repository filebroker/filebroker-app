package net.robinfriedli.filebroker.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val queryInput = view.findViewById<EditText>(R.id.queryInputHome)
        view.findViewById<Button>(R.id.searchButtonHome).setOnClickListener {
            val bundle = Bundle()
            bundle.putString("query", queryInput.text.toString())
            (activity as MainActivity).navHostFragment.navController.navigate(
                R.id.postsFragment,
                bundle
            )
        }

        return view
    }
}
