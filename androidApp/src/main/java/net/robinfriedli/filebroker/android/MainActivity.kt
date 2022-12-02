package net.robinfriedli.filebroker.android

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import net.robinfriedli.filebroker.Api

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColors(
            primary = Color(0xFFBB86FC),
            primaryVariant = Color(0xFF3700B3),
            secondary = Color(0xFF03DAC5)
        )
    } else {
        lightColors(
            primary = Color(0xFF6200EE),
            primaryVariant = Color(0xFF3700B3),
            secondary = Color(0xFF03DAC5)
        )
    }
    val typography = Typography(
        body1 = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        )
    )
    val shapes = Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(4.dp),
        large = RoundedCornerShape(0.dp)
    )

    MaterialTheme(
        colors = colors,
        typography = typography,
        shapes = shapes,
        content = content
    )
}

class MainActivity : AppCompatActivity() {

    val api: Api = Api()

    var drawerList: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "filebroker"

        val actionBar = supportActionBar
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            val drawerToggle = ActionBarDrawerToggle(this, drawer, toolbar, 0, 0)
            drawerToggle.isDrawerIndicatorEnabled = true
            drawer.addDrawerListener(drawerToggle)
            drawerToggle.syncState()
        }

        drawerList = findViewById(R.id.left_drawer)
        val navigationDrawerItemTitles =
            resources.getStringArray(R.array.navigation_drawer_items_array)
        drawerList!!.onItemClickListener =
            DrawerItemClickListener(drawer, drawerList!!, navigationDrawerItemTitles)

        setupDrawerItems()

        supportFragmentManager.beginTransaction().replace(R.id.content_frame, HomeFragment())
            .commit()
    }

    inner class DrawerItemClickListener(
        private val drawer: DrawerLayout,
        private val drawerList: ListView,
        private val navigationDrawerItemTitles: Array<String>
    ) : OnItemClickListener {
        override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            val fragment = when (p2) {
                0 -> HomeFragment()
                1 -> {
                    val currentLogin = api.currentLogin
                    if (currentLogin != null) {
                        ProfileFragment(api)
                    } else {
                        LoginFragment(api)
                    }
                }
                else -> null
            }

            if (fragment != null) {
                supportFragmentManager.beginTransaction().replace(R.id.content_frame, fragment)
                    .commit()

                drawerList.setItemChecked(p2, true)
                drawerList.setSelection(p2)
                title = navigationDrawerItemTitles[p2]
                drawer.closeDrawer(drawerList)
            }
        }

    }

    fun setupDrawerItems() {
        val currentLogin = api.currentLogin
        val profileDrawerItem = if (currentLogin != null) {
            DrawerItem(R.drawable.ic_baseline_person_24, currentLogin.user.user_name)
        } else {
            DrawerItem(R.drawable.ic_baseline_login_24, "Login")
        }

        val navigationDrawerItems = arrayOf(
            DrawerItem(R.drawable.ic_baseline_home_24, "Home"),
            profileDrawerItem
        )

        val navigationDrawerItemAdapter =
            DrawerItemAdapter(this, R.layout.list_view_item_row, navigationDrawerItems)
        drawerList!!.adapter = navigationDrawerItemAdapter
    }

    fun onLoginCompleted(redirectFragment: Fragment? = null) {
        if (redirectFragment != null) {
            supportFragmentManager.beginTransaction().replace(R.id.content_frame, redirectFragment)
                .commit()
        }

        if (drawerList != null) {
            setupDrawerItems()
        }
    }
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

@Composable
fun Greeting(text: String) {
    Text(text = text)
}

@Preview
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        Greeting("Hello, Android!")
    }
}
