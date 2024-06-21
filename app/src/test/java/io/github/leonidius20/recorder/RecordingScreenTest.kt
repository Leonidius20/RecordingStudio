package io.github.leonidius20.recorder

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecordingScreenTest {


    @Test
    fun `rec button changes to pause button when clicked`() {
        // todo: redo with https://robolectric.org/androidx_test/ (fragments section)
        // todo: we will also need to test that the button state is retained after
        // activity recreation or fragment being unattached/reattached (such
        // as when navigating to another fragment and back).
        // todo: so we are going to do hardcore TDD here and test all requirements
        // including saving state on recreations

        Robolectric.buildActivity(MainActivity::class.java).use { activityController ->
            activityController.setup()
            val activity = activityController.get()

            // get the rec button from binding?
        }
    }

}