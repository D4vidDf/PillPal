package com.d4viddf.medicationreminder.ui.screens

import androidx.activity.ComponentActivity
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
import androidx.compose.ui.test.assertDoesNotExist // For checking absence of nodes
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Expected English strings
    private val welcomeTitle = "Welcome to Medication Reminder!"
    private val appWelcomeSubtitle = "Your health companion for timely medication reminders." // New
    private val logoContentDescription = "App Logo"
    private val startOnboardingButtonText = "Start Onboarding" // New

    private val step1PagerTitle = "Getting Started"
    private val step1PagerDesc = "Here’s a quick tour of what you can do and the permissions we’ll need to make your experience seamless."
    private val nextButtonText = "Next"
    private val previousButtonText = "Previous"

    private fun setScreenSize(widthDp: Int, heightDp: Int) {
        val density = Density(density = 1f, fontScale = 1f)
        val configuration = android.content.res.Configuration().apply {
            screenWidthDp = widthDp; screenHeightDp = heightDp
        }

        composeTestRule.setContent {
            val navController = rememberNavController()
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val activity = context as? ComponentActivity ?: ComponentActivity()

            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDensity provides density,
                LocalViewConfiguration provides object : ViewConfiguration {
                    override val longPressTimeoutMillis: Long = 500
                    override val doubleTapTimeoutMillis: Long = 300
                    override val doubleTapMinTimeMillis: Long = 40
                    override val touchSlop: Float = 16f
                },
                LocalContext provides activity
            ) {
                OnboardingScreen(navController = navController)
            }
        }
    }

    @Test
    fun onboardingScreen_phoneLayout_initialDisplayCorrect_showsWelcomePage() {
        setScreenSize(widthDp = 360, heightDp = 640) // Phone

        // Check content of WelcomePageContent
        composeTestRule.onNodeWithContentDescription(logoContentDescription).assertIsDisplayed()
        composeTestRule.onNodeWithText(welcomeTitle).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(welcomeTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(appWelcomeSubtitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(startOnboardingButtonText).assertIsDisplayed()

        // Ensure pager-specific content is NOT initially visible on phone
        composeTestRule.onNodeWithText(step1PagerTitle, useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText(step1PagerDesc, useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText(nextButtonText).assertDoesNotExist()
        composeTestRule.onNodeWithText(previousButtonText).assertDoesNotExist()
    }

    @Test
    fun onboardingScreen_tabletLayout_initialDisplayCorrect_showsPagerContent() {
        setScreenSize(widthDp = 720, heightDp = 1280) // Tablet

        // Left Pane: Logo and Main Welcome Title (this is the overall screen title for tablet)
        composeTestRule.onNodeWithContentDescription(logoContentDescription, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(welcomeTitle, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(welcomeTitle, useUnmergedTree = true).assertIsDisplayed()


        // Right Pane: Pager with first item content (step1PagerTitle, step1PagerDesc)
        composeTestRule.onNodeWithText(step1PagerTitle, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(step1PagerTitle, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(step1PagerDesc, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(step1PagerDesc, useUnmergedTree = true).assertIsDisplayed()

        // Check navigation buttons for the pager
        composeTestRule.onNodeWithText(nextButtonText).assertIsDisplayed()
        composeTestRule.onNodeWithText(previousButtonText).assertDoesNotExist() // Previous not on first page of pager

        // Ensure phone-specific welcome page elements are NOT on tablet
        composeTestRule.onNodeWithText(startOnboardingButtonText).assertDoesNotExist()
        composeTestRule.onNodeWithText(appWelcomeSubtitle).assertDoesNotExist()
    }
}
