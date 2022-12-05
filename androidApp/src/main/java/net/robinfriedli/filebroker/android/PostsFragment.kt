package net.robinfriedli.filebroker.android

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.robinfriedli.filebroker.Api

class PostsFragment(var query: String? = null, var currentPage: Long = 0) :
    Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_posts, container, false)

        val queryInput = view.findViewById<EditText>(R.id.queryInputPosts)
        val searchButton = view.findViewById<Button>(R.id.searchButtonPosts)
        searchButton.setOnClickListener {
            val queryInputString = queryInput.text.toString()
            query = queryInputString.ifBlank {
                null
            }
            queryInput.hideKeyboard()
            executeSearch(view)
        }

        val queryArg = requireArguments().getString("query")
        if (!queryArg.isNullOrEmpty() && query.isNullOrEmpty()) {
            query = queryArg
            queryInput.setText(queryArg)
        }

        executeSearch(view)

        return view
    }

    fun executeSearch(view: View) {
        val imageGrid = view.findViewById<GridView>(R.id.imageGrid)
        GlobalScope.launch {
            requireActivity().runOnUiThread {
                val progressSpinner = view.findViewById<ProgressBar>(R.id.progressSpinner)
                progressSpinner.visibility = View.VISIBLE
            }
            try {
                val searchResult = try {
                    (activity as MainActivity).api.search(query, currentPage)
                } catch (e: Exception) {
                    val message = if (e is Api.InvalidHttpResponseException) {
                        val responseException = e as Api.InvalidHttpResponseException
                        if (responseException.status == 400) {
                            "Invalid Query"
                        } else {
                            Log.e(javaClass.simpleName, "Exception in search request", e)
                            "Search failed"
                        }
                    } else {
                        Log.e(javaClass.simpleName, "Exception in search request", e)
                        "Search failed"
                    }
                    requireActivity().runOnUiThread {
                        val alertBuilder = AlertDialog.Builder(context)
                        alertBuilder.setTitle("Error")
                        alertBuilder.setMessage(message)
                        alertBuilder.setPositiveButton(R.string.ok) { dialogInterface, _ -> dialogInterface.dismiss() }
                        alertBuilder.show()
                    }
                    return@launch
                }
                requireActivity().runOnUiThread {
                    imageGrid.adapter = ImageAdapterGridView(view.context, searchResult.posts)

                    val resultCountText = view.findViewById<TextView>(R.id.resultCount)
                    val pageNumberText = view.findViewById<TextView>(R.id.pageNumber)

                    val pageCount = searchResult.pages
                    val firstPage = currentPage < 1

                    resultCountText.text =
                        resources.getString(R.string.results, searchResult.full_count.toString())
                    pageNumberText.text = resources.getString(
                        R.string.page,
                        (currentPage + 1).toString(),
                        pageCount?.toString() ?: "?"
                    )

                    val buttonRow = view.findViewById<LinearLayout>(R.id.paginationButtonRow)
                    buttonRow.removeAllViews()

                    buttonRow.addView(
                        createPaginationButton(
                            view,
                            context,
                            -2,
                            0,
                            resources.getString(R.string.first),
                            !firstPage
                        )
                    )
                    buttonRow.addView(
                        createPaginationButton(
                            view,
                            context,
                            -1,
                            currentPage - 1,
                            resources.getString(R.string.prev),
                            !firstPage
                        )
                    )

                    if (pageCount != null) {
                        val lastPage = currentPage >= pageCount - 1
                        buttonRow.addView(
                            createPaginationButton(
                                view,
                                context,
                                -3,
                                currentPage + 1,
                                resources.getString(R.string.next),
                                !lastPage
                            )
                        )
                        buttonRow.addView(
                            createPaginationButton(
                                view,
                                context,
                                -4,
                                pageCount - 1,
                                resources.getString(R.string.last),
                                !lastPage
                            )
                        )
                    } else {
                        buttonRow.addView(
                            createPaginationButton(
                                view,
                                context,
                                -3,
                                currentPage + 1,
                                resources.getString(R.string.next),
                                true
                            )
                        )
                    }
                }
            } finally {
                requireActivity().runOnUiThread {
                    val progressSpinner = view.findViewById<ProgressBar>(R.id.progressSpinner)
                    progressSpinner.visibility = View.GONE
                }
            }
        }
    }

    fun createPaginationButton(
        view: View,
        context: Context?,
        id: Int,
        page: Long,
        text: String,
        isEnabled: Boolean,
        selected: Boolean = false
    ): Button {
        val button = Button(context)
        button.id = id
        button.text = text
        button.isEnabled = isEnabled

        if (selected) {
            button.setTextColor(Color.parseColor("#FFFFFF"))
        }

        button.setOnClickListener {
            currentPage = page
            executeSearch(view)
        }

        return button
    }

    inner class ImageAdapterGridView(val context: Context, val posts: List<Api.PostQueryObject>) :
        BaseAdapter() {
        override fun getCount(): Int {
            return posts.size
        }

        override fun getItem(p0: Int): Any {
            return posts[p0]
        }

        override fun getItemId(p0: Int): Long {
            return posts[p0].pk.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val post = posts[position]

            val imageView = if (convertView == null) {
                val imageView = ImageView(context)
                imageView.layoutParams = ViewGroup.LayoutParams(320, 180)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.setPadding(16, 16, 16, 16)
                imageView.setOnClickListener {
                    val bundle = Bundle()
                    bundle.putInt("key", post.pk)
                    bundle.putString("query", query)
                    bundle.putLong("currentPage", currentPage)
                    (activity as MainActivity).navHostFragment.navController.navigate(
                        R.id.postDetailFragment,
                        bundle
                    )
                }
                imageView
            } else {
                convertView as ImageView
            }

            val thumbnailUrl = if (post.thumbnail_url != null) {
                post.thumbnail_url
            } else if (post.thumbnail_object_key != null) {
                Api.BASE_URL + "get-object/" + post.thumbnail_object_key
            } else {
                null
            }

            if (thumbnailUrl != null) {
                Picasso.get().load(thumbnailUrl).into(imageView)
            } else {
                imageView.setImageResource(R.drawable.logo512)
            }

            return imageView
        }

    }
}