package com.naampath.colorpath3d

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NavigationInstrumentedTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hilt.inject()
    }

    @Test
    fun menuButtonsOpenTheirScreens() {
        compose.waitUntil(timeoutMillis = 8_000) {
            compose.onAllNodesWithText("PLAY").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("SETTINGS").performClick()
        compose.onNodeWithText("Music").assertIsDisplayed()
        compose.onNodeWithText("BACK").performClick()
        compose.onNodeWithText("LEVELS").performClick()
        compose.onNodeWithText("Neon Garden").assertIsDisplayed()
    }
}
