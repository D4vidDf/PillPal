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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import org.junit.Before // For @Before setup
import androidx.test.core.app.ApplicationProvider // For context

// Minimal configuration for Robolectric to work with Compose
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Expected English strings
    private val welcomeTitle = "Welcome to Medication Reminder!" // Main title, outside pager
    private val logoContentDescription = "App Logo"
    private val nextButtonText = "Next"
    private val previousButtonText = "Previous" // New
    private val step1PagerTitle = "Getting Started" // New: Title for first pager item
    private val step1PagerDesc = "Here’s a quick tour of what you can do and the permissions we’ll need to make your experience seamless." // New: Desc for first pager item


    @Before
    fun setup() {
        // Common setup for providing NavController and Context for OnboardingScreen
        // This avoids repeating it in setScreenSize if OnboardingScreen is the direct content.
        // However, setScreenSize re-sets content, so it's better there or pass NavController.
        // Let's keep it in setScreenSize for now as it re-invokes setContent.
    }

    // Helper to set screen size for tests
    private fun setScreenSize(widthDp: Int, heightDp: Int) {
        val density = Density(density = 1f, fontScale = 1f)
        val configuration = android.content.res.Configuration().apply {
            screenWidthDp = widthDp
            screenHeightDp = heightDp
        }

        composeTestRule.setContent {
            val navController = rememberNavController()
            // Use ApplicationProvider for a generic context suitable for most UI tests not needing Activity specifics.
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            // OnboardingScreen expects a ComponentActivity. For tests not directly calling activity methods,
            // a simple cast might work if not interacting with lifecycle-dependent parts.
            // Or, use Robolectric to create a dummy activity.
            // For now, assuming PermissionUtils calls are what need ComponentActivity, and they aren't directly tested here.
            // The preview code used `currentContext as? ComponentActivity ?: ComponentActivity()`.
            // Let's use a TestActivity if needed, or ensure context is ComponentActivity.
            // For simplicity, if LocalContext.current in test is not ComponentActivity, this may fail if cast is strict.
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
                LocalContext provides activity // Provide activity context
            ) {
                OnboardingScreen(navController = navController)
            }
        }
    }

    @Test
    fun onboardingScreen_phoneLayout_initialDisplayCorrect() {
        setScreenSize(widthDp = 360, heightDp = 640)

        // Check logo
        composeTestRule.onNodeWithContentDescription(logoContentDescription).assertIsDisplayed()

        // Check main title (outside pager)
        composeTestRule.onNodeWithText(welcomeTitle).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(welcomeTitle).assertIsDisplayed()

        // Check content of the first pager item
        composeTestRule.onNodeWithText(step1PagerTitle, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(step1PagerTitle, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(step1PagerDesc, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(step1PagerDesc, useUnmergedTree = true).assertIsDisplayed()

        // Check navigation buttons
        composeTestRule.onNodeWithText(nextButtonText).assertIsDisplayed()
        composeTestRule.onNodeWithText(previousButtonText).assertDoesNotExist() // Previous not on first page
    }

    @Test
    fun onboardingScreen_tabletLayout_initialDisplayCorrect() {
        setScreenSize(widthDp = 720, heightDp = 1280)

        // Left Pane: Logo and Main Title
        composeTestRule.onNodeWithContentDescription(logoContentDescription, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(welcomeTitle, useUnmergedTree = true).assertIsDisplayed() // Main title
        composeTestRule.onNodeWithContentDescription(welcomeTitle, useUnmergedTree = true).assertIsDisplayed()

        // Right Pane: Pager with first item content
        composeTestRule.onNodeWithText(step1PagerTitle, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(step1PagerTitle, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(step1PagerDesc, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(step1PagerDesc, useUnmergedTree = true).assertIsDisplayed()

        // Check navigation buttons
        composeTestRule.onNodeWithText(nextButtonText).assertIsDisplayed()
        composeTestRule.onNodeWithText(previousButtonText).assertDoesNotExist() // Previous not on first page
    }
}
