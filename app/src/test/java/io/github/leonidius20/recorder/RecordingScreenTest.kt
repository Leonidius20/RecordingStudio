package io.github.leonidius20.recorder

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecordingScreenTest {


    @Test
    fun `rec button changes to pause button when clicked`() {

        // todo: we will also need to test that the button state is retained after
        // activity recreation or fragment being unattached/reattached (such
        // as when navigating to another fragment and back).
        // todo: so we are going to do hardcore TDD here and test all requirements
        // including saving state on recreations
        /*val scenario = launchFragmentInHiltContainer<HomeFragment>()

        onView(withId(R.id.recordButton)).check(

            ViewAssertions.matches(
                withTagValue(IsEqual(BTN_IMG_TAG_RECORD))
            )
        )

        onView(withId(R.id.recordButton)).perform(click())*/

        // todo: click on permission dialog? also test what happens if you don't click?

        // todo: use ui automator and instrumented test for confirming the permission dialog
        /*onView(withId(R.id.recordButton)).check(
            ViewAssertions.matches(
                withTagValue(IsEqual(BTN_IMG_TAG_PAUSE))
            )
        )

        onView(withId(R.id.recordButton)).perform(click())

        onView(withId(R.id.recordButton)).check(

            ViewAssertions.matches(
                withTagValue(IsEqual(BTN_IMG_TAG_RECORD))
            )
        )*/
    }

}