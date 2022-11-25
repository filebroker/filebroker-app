package net.robinfriedli.filebroker.android

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView


class DrawerItem(val icon: Int, val name: String)

class DrawerItemHolder(val imageView: ImageView, val textView: TextView)

class DrawerItemAdapter(
    context: Context,
    private val layoutResourceId: Int,
    private val items: Array<DrawerItem>
) : ArrayAdapter<DrawerItem>(
    context,
    layoutResourceId,
    items
) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val drawerItemHolder = if (view == null) {
            val layoutInflater = (context as Activity).layoutInflater
            view = layoutInflater.inflate(layoutResourceId, parent, false)

            val imageViewIcon = view.findViewById<ImageView>(R.id.imageViewIcon)
            val textViewName = view.findViewById<TextView>(R.id.textViewName)

            val drawerItemHolder = DrawerItemHolder(imageViewIcon, textViewName)
            view.tag = drawerItemHolder
            drawerItemHolder
        } else {
            view.tag as DrawerItemHolder
        }

        val drawerItem = items[position]

        drawerItemHolder.imageView.setImageResource(drawerItem.icon)
        drawerItemHolder.textView.text = drawerItem.name

        return view!!
    }
}
