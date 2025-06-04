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

// Minimal configuration for Robolectric to work with Compose
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Expected English strings
    private val welcomeTitle = "Welcome to Medication Reminder!" // Used for step 1 title
    private val welcomeSubtitle = "Let's get you set up." // Used for step 1 desc
    // private val stepsPlaceholder = "[Placeholder for Onboarding Steps]" // No longer directly visible initially
    // private val permissionsPlaceholder = "[Placeholder for Permission Settings]" // No longer directly visible initially
    private val welcomeAreaPaneDescription = "Welcome message area"
    // private val stepsPermissionsPaneDescription = "Onboarding steps and permissions area" // This might need re-evaluation as the right pane now hosts the pager
    private val logoContentDescription = "App Logo"
    private val nextButtonText = "Next"


    // Helper to set screen size for tests
    private fun setScreenSize(widthDp: Int, heightDp: Int, isTablet: Boolean = false) {
        val density = Density(density = 1f, fontScale = 1f)
        val configuration = android.content.res.Configuration().apply {
            screenWidthDp = widthDp
            screenHeightDp = heightDp
        }

        composeTestRule.setContent {
            // For previews/tests, LocalContext.current might not be a ComponentActivity.
            // Providing a mock or simple Activity if PermissionUtils doesn't strictly need ComponentActivity methods.
            // However, OnboardingScreen now expects NavController.
            val navController = rememberNavController()
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDensity provides density,
                LocalViewConfiguration provides object : ViewConfiguration {
                    override val longPressTimeoutMillis: Long = 500
                    override val doubleTapTimeoutMillis: Long = 300
                    override val doubleTapMinTimeMillis: Long = 40
                    override val touchSlop: Float = 16f
                },
                // Provide a basic Activity context for the test if needed by underlying code
                // This might need to be a mock ComponentActivity if PermissionUtils is strict
                LocalContext provides androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            ) {
                OnboardingScreen(navController = navController)
            }
        }
    }

    @Test
    fun onboardingScreen_phoneLayout_isDisplayed_onSmallScreen_withContentDescriptions() {
        setScreenSize(widthDp = 360, heightDp = 640)

        composeTestRule.onNodeWithContentDescription(logoContentDescription).assertIsDisplayed()

        // These texts are part of the main layout on phone, not inside pager's first page by current design
        // The pager itself contains steps starting from step1_title/desc
        // Actually, the main welcome title is outside, the pager contains all steps including the first one.
        composeTestRule.onNodeWithText(welcomeTitle).assertIsDisplayed() // Main title
        composeTestRule.onNodeWithContentDescription(welcomeTitle).assertIsDisplayed()

        // The first page of the pager will contain the first step's content.
        // (R.string.onboarding_step1_title which is onboarding_welcome_title,
        // and R.string.onboarding_step1_desc which is onboarding_welcome_subtitle)
        // So checking for welcomeSubtitle which is part of the first page of the pager.
        // To be very precise, we should check for the title *within* the pager's first page.
        // However, since welcomeTitle is also the title of the first page, this is a bit complex to distinguish.
        // For now, let's assume the main title check is sufficient for the "Welcome" text.
        // The description of the first step (welcomeSubtitle) should be findable.
        composeTestRule.onNodeWithText(welcomeSubtitle, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(welcomeSubtitle, useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithText(nextButtonText).assertIsDisplayed()
    }

    @Test
    fun onboardingScreen_tabletLayout_isDisplayed_onLargeScreen_withContentDescriptions() {
        setScreenSize(widthDp = 720, heightDp = 1280, isTablet = true)

        // Left Pane
        composeTestRule.onNodeWithContentDescription(welcomeAreaPaneDescription).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(logoContentDescription, useUnmergedTree = true).assertIsDisplayed() // Logo is inside left pane
        composeTestRule.onNodeWithText(welcomeTitle, useUnmergedTree = true).assertIsDisplayed() // Main title in left pane
        composeTestRule.onNodeWithContentDescription(welcomeTitle, useUnmergedTree = true).assertIsDisplayed()

        // Right Pane hosts the pager. The content of the first page should be visible.
        // The first page uses R.string.onboarding_step1_title (== welcomeTitle) and R.string.onboarding_step1_desc (== welcomeSubtitle)
        // The title of the first page (welcomeTitle) should be visible.
        // Since welcomeTitle is also in the left pane, we need to be careful.
        // Let's check for the description (welcomeSubtitle) in the right pane (pager area).
        composeTestRule.onNodeWithText(welcomeSubtitle, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(welcomeSubtitle, useUnmergedTree = true).assertIsDisplayed()
        // We could also check for the right pane's own content description if it had one,
        // but stepsPermissionsPaneDescription was for the old layout. The new right pane is just a Column hosting the pager.

        composeTestRule.onNodeWithText(nextButtonText).assertIsDisplayed()
    }
}
