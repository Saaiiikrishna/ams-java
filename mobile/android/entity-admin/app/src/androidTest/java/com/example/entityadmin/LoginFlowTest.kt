package com.example.entityadmin

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.entityadmin.ui.MainActivity // Corrected import for MainActivity if it's in ui package
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class LoginFlowTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(MainActivity::class.java) // MainActivity is in com.example.entityadmin

    @Test
    fun successfulLogin_navigateToSessionList() {
        // Type username
        onView(withId(R.id.editUsername))
            .perform(typeText("testadmin"), closeSoftKeyboard())

        // Type password
        onView(withId(R.id.editPassword))
            .perform(typeText("password"), closeSoftKeyboard())

        // Click login button
        onView(withId(R.id.buttonLogin))
            .perform(click())

        // Wait for navigation and check if SessionListFragment is displayed
        // This delay is a temporary workaround for real-world flakiness.
        // Proper solution involves IdlingResource.
        Thread.sleep(2000) // Consider this a placeholder for a more robust synchronization mechanism

        // Check for a view unique to SessionListFragment
        onView(withId(R.id.fabAddSession))
            .check(matches(isDisplayed()))
    }
}
