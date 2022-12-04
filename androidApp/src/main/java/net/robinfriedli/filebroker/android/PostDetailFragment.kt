package net.robinfriedli.filebroker.android

import android.app.AlertDialog
import android.net.Uri
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

class PostDetailFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_post_detail, container, false)

        val key = requireArguments().getInt("key")
        val query = requireArguments().getString("query")
        val currentPage = requireArguments().getLong("currentPage")

        val progressSpinner = view.findViewById<ProgressBar>(R.id.progressSpinner)
        progressSpinner.visibility = View.VISIBLE

        val prevPostButton = view.findViewById<Button>(R.id.prevPost)
        val nextPostButton = view.findViewById<Button>(R.id.nextPost)

        prevPostButton.visibility = View.GONE
        nextPostButton.visibility = View.GONE

        val postContent = view.findViewById<FrameLayout>(R.id.postContent)
        GlobalScope.launch {
            val post = try {
                (activity as MainActivity).api.getPost(key, query, currentPage)
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "Failed to load post", e)
                requireActivity().runOnUiThread {
                    val alertBuilder = AlertDialog.Builder(context)
                    alertBuilder.setTitle("Error")
                    alertBuilder.setMessage("Failed to load post")
                    alertBuilder.setPositiveButton(R.string.ok) { dialogInterface, _ -> dialogInterface.dismiss() }
                    alertBuilder.show()
                }
                return@launch
            } finally {
                requireActivity().runOnUiThread {
                    val progressSpinner = view.findViewById<ProgressBar>(R.id.progressSpinner)
                    progressSpinner.visibility = View.GONE
                }
            }

            fun switchToPost(key: Int) {
                val bundle = Bundle()
                bundle.putInt("key", key)
                bundle.putString("query", query)
                bundle.putLong("currentPage", currentPage)
                (activity as MainActivity).navHostFragment.navController.navigate(
                    R.id.postDetailFragment,
                    bundle
                )
            }

            requireActivity().runOnUiThread {
                if (post.prev_post_pk != null) {
                    prevPostButton.visibility = View.VISIBLE
                    prevPostButton.setOnClickListener {
                        switchToPost(post.prev_post_pk!!)
                    }
                }
                if (post.next_post_pk != null) {
                    nextPostButton.visibility = View.VISIBLE
                    nextPostButton.setOnClickListener {
                        switchToPost(post.next_post_pk!!)
                    }
                }

                val postTagsText = view.findViewById<TextView>(R.id.postTags)
                val postTitle = view.findViewById<TextView>(R.id.postTitle)
                val postDescription = view.findViewById<TextView>(R.id.postDescription)

                postTagsText.text =
                    post.tags.joinToString(separator = ", ", transform = { tag -> tag.tag_name })
                postTitle.text = post.title
                postDescription.text = post.description
            }

            requireActivity().runOnUiThread {
                val contentView: View =
                    if (post.s3_object != null && post.s3_object!!.mime_type.startsWith("image")) {
                        val imageView = ImageView(context)
                        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        imageView.adjustViewBounds = true
                        Picasso.get()
                            .load(Api.BASE_URL + "get-object/" + post.s3_object!!.object_key)
                            .into(imageView)
                        imageView
                    } else if (post.s3_object != null && post.s3_object!!.mime_type.startsWith("video")) {
                        val videoView = VideoView(context)
                        videoView.setVideoURI(Uri.parse(Api.BASE_URL + "get-object/" + post.s3_object!!.object_key))
                        val mediaController = MediaController(context)
                        videoView.setMediaController(mediaController)
                        mediaController.setAnchorView(videoView)
                        videoView.start()
                        videoView
                    } else {
                        val textView = TextView(context)
                        textView.text = resources.getString(R.string.unable_to_display_data)
                        textView
                    }
                postContent.addView(contentView)
            }
        }

        return view
    }
}
