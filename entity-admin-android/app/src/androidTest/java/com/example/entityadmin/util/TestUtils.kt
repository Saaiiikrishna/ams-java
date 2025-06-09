package com.example.entityadmin.util // Or your test util package

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

object TestUtils {
    fun clickChildViewWithId(id: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View>? {
                return null // Note: It's good practice to set appropriate constraints.
                            // For example, ViewMatchers.isAssignableFrom(ViewGroup::class.java)
                            // if the parent view must be a ViewGroup.
            }

            override fun getDescription(): String {
                return "Click on a child view with specified id."
            }

            override fun perform(uiController: UiController, view: View) {
                val v = view.findViewById<View>(id)
                v.performClick()
            }
        }
    }
}
