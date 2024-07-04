package io.github.leonidius20.recorder

import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
* launchFragmentInContainer from the androidx.fragment:fragment-testing library
* is NOT possible to use right now as it uses a hardcoded Activity under the hood
* which is not annotated with @AndroidEntryPoint for Hilt. For a workaround this
 * activity is used.
**/
@AndroidEntryPoint
class HiltTestActivity : AppCompatActivity()