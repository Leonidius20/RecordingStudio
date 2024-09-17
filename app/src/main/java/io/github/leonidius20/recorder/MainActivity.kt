package io.github.leonidius20.recorder

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.databinding.ActivityMainBinding

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            /*navigationBarStyle = SystemBarStyle.auto(
                lightScrim = R.color.md_theme_primary,
                darkScrim = R.color.md_theme_primary
            )*/
        )
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsetsListener(binding.root)

        // todo: https://developer.android.com/guide/navigation/testing
        val navView: NavigationBarView = binding.navView as NavigationBarView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_record, R.id.navigation_recordings_list, R.id.navigation_settings
            )
        )
        // setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    }

    private fun applyWindowInsetsListener(rootView: View) {
        val orientation = resources.configuration.orientation
        val layoutDirection = resources.configuration.layoutDirection

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            // Apply the insets as a margin to the view. This solution sets
            // only the bottom, left, and right dimensions, but you can apply whichever
            // insets are appropriate to your layout. You can also update the view padding
            // if that's more appropriate.


            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top

                /*
                 * we handle left margin ourselves if there is no nav rail there.
                 * if there is, the rail will handle insets itself.
                 * there is no nav rail there if we are in portrait mode, or in RTL
                 * layout (e.g. hebrew), bc in this case the rail is on the right.
                 */
                if (orientation == Configuration.ORIENTATION_PORTRAIT || resources.configuration.layoutDirection == Configuration.SCREENLAYOUT_LAYOUTDIR_RTL) {
                    leftMargin = insets.left
                }

                /*
                 * we handle right margin ourselves if there is no nav rail there.
                 * if there is, the rail will handle insets itself.
                 * there is no nav rail there if we are in portrait mode, or in LTR
                 * layout (default, e.g. for English), bc in this case the rail is on the left.
                 */
                if (orientation == Configuration.ORIENTATION_PORTRAIT || resources.configuration.layoutDirection == Configuration.SCREENLAYOUT_LAYOUTDIR_LTR) {
                    rightMargin = insets.right
                }

                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    bottomMargin = insets.bottom // (handled by bottom app bar in vertical mode).
                }
            }

            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            //WindowInsetsCompat.CONSUMED
            windowInsets
        }

        // this does not bloody work!
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

            val navRail = binding.navView

            ViewCompat.setOnApplyWindowInsetsListener(navRail) { v, windowInsets ->

                /*val insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.displayCutout()
                )*/

                windowInsets.displayCutout?.let { cutout ->
                    // default, rail on left
                    if (resources.configuration.layoutDirection == Configuration.SCREENLAYOUT_LAYOUTDIR_LTR) {
                        v.updatePadding(
                            left = cutout.safeInsetLeft
                        )
                    } else {
                        // rtl, hebrew
                        v.updatePadding(
                            right = cutout.safeInsetRight
                        )
                    }
                }

                windowInsets
            }
        }

    }

}