package com.example.entityadmin

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.entityadmin.ui.MainActivity // Corrected path
import com.example.entityadmin.util.TestUtils // Import for custom ViewAction
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
// import org.hamcrest.Matchers.allOf // Not used in final version of provided code
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import androidx.test.platform.app.InstrumentationRegistry // For context menu

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class SubscriberManagementFlowsTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val uniqueNameSuffix = UUID.randomUUID().toString().substring(0, 6)

    @Before
    fun setup() {
        // Login
        onView(withId(R.id.editUsername)).perform(typeText("testadmin"), closeSoftKeyboard())
        onView(withId(R.id.editPassword)).perform(typeText("password"), closeSoftKeyboard())
        onView(withId(R.id.buttonLogin)).perform(click())
        Thread.sleep(1500) // Wait for login

        // Navigate to SubscriberListFragment from SessionListFragment's menu
        try {
             androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
             Thread.sleep(200)
             onView(withText("Manage Subscribers")).perform(click())
        } catch (e: Exception) {
            System.err.println("Could not perform overflow menu click for Manage Subscribers: " + e.message)
            // Fallback for specific devices/setups if standard overflow fails
             try {
                onView(withContentDescription("More options")).perform(click())
                Thread.sleep(200)
                onView(withText("Manage Subscribers")).perform(click())
             } catch (e2: Exception) {
                System.err.println("Fallback 'More options' click for Manage Subscribers also failed: " + e2.message)
                // This part remains tricky for pure Espresso. Consider UI Automator or simpler navigation for tests.
             }
        }
        Thread.sleep(1000) // Wait for navigation
        // Verify SubscriberListFragment is displayed
        onView(withId(R.id.fabAddSubscriber)).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToSubscriberList() {
        // This test essentially verifies the @Before setup worked.
        onView(withId(R.id.recyclerViewSubscribers)).check(matches(isDisplayed()))
        onView(withId(R.id.fabAddSubscriber)).check(matches(isDisplayed()))
    }

    @Test
    fun testAddSubscriberFlow() {
        // From SubscriberListFragment (already here due to @Before)
        onView(withId(R.id.fabAddSubscriber)).perform(click())
        Thread.sleep(500) // Wait for navigation

        // Verify AddEditSubscriberFragment is displayed (check for a unique view)
        onView(withId(R.id.editTextSubscriberName)).check(matches(isDisplayed()))
        // Check title (label in nav_graph is "{title}", default "Add Subscriber")
        // To check ActionBar title, you'd do:
        // onView(allOf(isAssignableFrom(TextView::class.java), withParent(isAssignableFrom(Toolbar::class.java))))
        //    .check(matches(withText("Add Subscriber")))
        // For now, relying on default or checking a view with that text if available.
        // The fragment label itself is set to "Add Subscriber" by default in nav args.
        // We can check a view that might display this, or assume based on other elements.

        // Enter subscriber details
        val subscriberName = "TestSub $uniqueNameSuffix"
        val subscriberEmail = "testsub_$uniqueNameSuffix@example.com"
        val nfcUid = "NFC_$uniqueNameSuffix"

        onView(withId(R.id.editTextSubscriberName)).perform(typeText(subscriberName), closeSoftKeyboard())
        onView(withId(R.id.editTextSubscriberEmail)).perform(typeText(subscriberEmail), closeSoftKeyboard())
        onView(withId(R.id.editTextNfcCardUid)).perform(typeText(nfcUid), closeSoftKeyboard())

        // Click save button
        onView(withId(R.id.buttonSaveSubscriber)).perform(click())
        Thread.sleep(2000) // Wait for save and navigation back (increased from 1500 for network op)

        // Verify navigation back to SubscriberListFragment
        onView(withId(R.id.fabAddSubscriber)).check(matches(isDisplayed()))

        // Optional: Verify new subscriber in the list
        // onView(withId(R.id.recyclerViewSubscribers))
        //    .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
        //        hasDescendant(withText(subscriberName))
        //    ))
        // onView(withText(subscriberName)).check(matches(isDisplayed()))
    }

    @Test
    fun testEditSubscriberNavigation() {
        // Add a subscriber first to ensure one exists for editing
        onView(withId(R.id.fabAddSubscriber)).perform(click())
        Thread.sleep(500)
        val editSubName = "EditSub $uniqueNameSuffix"
        val editSubEmail = "editsub_$uniqueNameSuffix@example.com"
        onView(withId(R.id.editTextSubscriberName)).perform(typeText(editSubName), closeSoftKeyboard())
        onView(withId(R.id.editTextSubscriberEmail)).perform(typeText(editSubEmail), closeSoftKeyboard())
        onView(withId(R.id.buttonSaveSubscriber)).perform(click())
        Thread.sleep(2000) // Wait for save

        // Click on the item containing the subscriber we just added.
        // This is still brittle without IdlingResources or specific item matching.
        // For simplicity, clicking first item, assuming it's the one just added (if list was empty).
        onView(withId(R.id.recyclerViewSubscribers))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        Thread.sleep(500) // Wait for navigation

        // Verify AddEditSubscriberFragment is displayed in "Edit" mode
        onView(withId(R.id.editTextSubscriberName)).check(matches(isDisplayed()))
        // Check title. The title is passed as nav arg. How it's displayed depends on Toolbar setup.
        // If Activity title is set:
        // activityRule.scenario.onActivity { activity ->
        //    assertEquals("Edit Subscriber", activity.title.toString())
        // }
        // Or if a TextView in the fragment displays it. For now, assume it's "Edit Subscriber".
        // A simple check could be if the pre-filled name matches.
        onView(withId(R.id.editTextSubscriberName)).check(matches(withText(editSubName)))
    }

    @Test
    fun testDeleteSubscriberDialogIsShown() {
        // Add a subscriber first to ensure one exists for deletion
        onView(withId(R.id.fabAddSubscriber)).perform(click())
        Thread.sleep(500)
        val delSubName = "DelSub $uniqueNameSuffix"
        val delSubEmail = "delsub_$uniqueNameSuffix@example.com"
        onView(withId(R.id.editTextSubscriberName)).perform(typeText(delSubName), closeSoftKeyboard())
        onView(withId(R.id.editTextSubscriberEmail)).perform(typeText(delSubEmail), closeSoftKeyboard())
        onView(withId(R.id.buttonSaveSubscriber)).perform(click())
        Thread.sleep(2000) // Wait for save

        // Click the delete button on the first item
        onView(withId(R.id.recyclerViewSubscribers))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, TestUtils.clickChildViewWithId(R.id.buttonDeleteSubscriber)
            ))
        Thread.sleep(500) // Wait for dialog

        // Verify AlertDialog is shown
        onView(withText("Delete Subscriber")).check(matches(isDisplayed())) // Corrected Dialog Title
        onView(withText("Are you sure you want to delete this subscriber?")).check(matches(isDisplayed()))
        onView(withId(android.R.id.button1)).check(matches(withText("Delete"))) // Positive button
        onView(withId(android.R.id.button2)).check(matches(withText("Cancel"))) // Negative button

        // Click "Cancel" to dismiss
        onView(withId(android.R.id.button2)).perform(click()) // Click Cancel
        Thread.sleep(200)
        onView(withId(R.id.fabAddSubscriber)).check(matches(isDisplayed())) // Back on list
    }
}
