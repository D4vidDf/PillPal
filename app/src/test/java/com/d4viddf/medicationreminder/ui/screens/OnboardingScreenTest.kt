package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription // Import for content description assertions

// Minimal configuration for Robolectric to work with Compose
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28]) // SDK 28 is commonly used for Robolectric tests
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Expected English strings (mirroring strings.xml for testing)
    private val welcomeTitle = "Welcome to Medication Reminder!"
    private val welcomeSubtitle = "Let's get you set up."
    private val stepsPlaceholder = "[Placeholder for Onboarding Steps]"
    private val permissionsPlaceholder = "[Placeholder for Permission Settings]"
    private val welcomeAreaPaneDescription = "Welcome message area"
    private val stepsPermissionsPaneDescription = "Onboarding steps and permissions area"

    // Helper to set screen size for tests
    private fun setScreenSize(widthDp: Int, heightDp: Int) {
        val density = Density(density = 1f, fontScale = 1f)
        // Use androidx.compose.ui.util.TestConfiguration if available,
        // otherwise, a simple Configuration object.
        // For this environment, directly creating Configuration for LocalConfiguration.
        val configuration = android.content.res.Configuration().apply {
            screenWidthDp = widthDp
            screenHeightDp = heightDp
        }

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDensity provides density,
                LocalViewConfiguration provides object : ViewConfiguration {
                    override val longPressTimeoutMillis: Long = 500
                    override val doubleTapTimeoutMillis: Long = 300
                    override val doubleTapMinTimeMillis: Long = 40
                    override val touchSlop: Float = 16f
                }
            ) {
                OnboardingScreen() // Assuming OnboardingScreen uses stringResource internally
            }
        }
    }

    @Test
    fun onboardingScreen_phoneLayout_isDisplayed_onSmallScreen_withContentDescriptions() {
        setScreenSize(widthDp = 360, heightDp = 640)

        // Check for text and its content description
        composeTestRule.onNodeWithText(welcomeTitle).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(welcomeTitle).assertIsDisplayed()

        composeTestRule.onNodeWithText(welcomeSubtitle).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(welcomeSubtitle).assertIsDisplayed()

        composeTestRule.onNodeWithText(stepsPlaceholder).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(stepsPlaceholder).assertIsDisplayed()

        composeTestRule.onNodeWithText(permissionsPlaceholder).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(permissionsPlaceholder).assertIsDisplayed()
    }

    @Test
    fun onboardingScreen_tabletLayout_isDisplayed_onLargeScreen_withContentDescriptions() {
        setScreenSize(widthDp = 720, heightDp = 1280)

        // Check for elements unique to the tablet layout
        // Left Pane
        composeTestRule.onNodeWithContentDescription(welcomeAreaPaneDescription).assertIsDisplayed()
        composeTestRule.onNodeWithText(welcomeTitle, useUnmergedTree = true).assertIsDisplayed() // useUnmergedTree might be needed if text is within the pane node
        composeTestRule.onNodeWithContentDescription(welcomeTitle, useUnmergedTree = true).assertIsDisplayed()

        // Right Pane
        composeTestRule.onNodeWithContentDescription(stepsPermissionsPaneDescription).assertIsDisplayed()
        composeTestRule.onNodeWithText(welcomeSubtitle, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(welcomeSubtitle, useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithText(stepsPlaceholder, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(stepsPlaceholder, useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithText(permissionsPlaceholder, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(permissionsPlaceholder, useUnmergedTree = true).assertIsDisplayed()
    }
}
