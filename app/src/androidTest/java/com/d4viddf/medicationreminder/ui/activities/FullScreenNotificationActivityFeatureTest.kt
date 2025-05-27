package com.d4viddf.medicationreminder.ui.activities

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.components.ShapeType // Required for FullScreenNotificationScreen
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

// Not using Hilt for this composable-focused test for simplicity,
// as FullScreenNotificationScreen's dependencies can be directly provided.

class FullScreenNotificationActivityFeatureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Attempting to load real string resources
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val medicationName = "TestMed"
    private val medicationDosage = "1 dose"
    private val medicationTypeName = "Pill" // This matches one of the cases in MedicationTypeImage
    private val reminderId = 1 // Fixed ID for deterministic shape selection

    // String resources fetched using InstrumentationRegistry
    private val markAsTakenText = targetContext.getString(R.string.fullscreen_notification_action_taken)
    private val dismissText = targetContext.getString(R.string.fullscreen_notification_action_dismiss)
    private val tickContentDescription = targetContext.getString(R.string.content_description_tick_mark)
    private val pillIconContentDescription = targetContext.getString(R.string.medication_type_pill_image_description)


    @Test
    fun initialDisplay_showsMedicationInfoAndActions_noTick_andMedicationIcon() {
        composeTestRule.setContent {
            AppTheme {
                FullScreenNotificationScreen(
                    reminderId = reminderId,
                    medicationName = medicationName,
                    medicationDosage = medicationDosage,
                    medicationTypeName = medicationTypeName, // "Pill"
                    backgroundColor = Color.White,
                    contentColor = Color.Black,
                    onMarkAsTaken = {},
                    onDismiss = {}
                )
            }
        }

        // Verify initial texts and buttons are displayed
        composeTestRule.onNodeWithText(medicationName).assertIsDisplayed()
        composeTestRule.onNodeWithText(medicationDosage).assertIsDisplayed()
        composeTestRule.onNodeWithText(markAsTakenText, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(dismissText, useUnmergedTree = true).assertIsDisplayed()

        // Verify MedicationTypeImage is displayed (by checking its specific medication icon's content description)
        // This also implies AnimatedShapeBackground is present as it's part of MedicationTypeImage
        composeTestRule.onNodeWithContentDescription(pillIconContentDescription, useUnmergedTree = true).assertIsDisplayed()

        // Verify tick is not shown initially
        composeTestRule.onNodeWithContentDescription(tickContentDescription, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun markAsTaken_hidesInfoAndActions_showsTick() {
        // State for onMarkAsTaken to ensure it's callable and triggers recomposition correctly
        var markedAsTakenCalled = false

        composeTestRule.setContent {
            AppTheme {
                FullScreenNotificationScreen(
                    reminderId = reminderId,
                    medicationName = medicationName,
                    medicationDosage = medicationDosage,
                    medicationTypeName = medicationTypeName,
                    backgroundColor = Color.White,
                    contentColor = Color.Black,
                    onMarkAsTaken = { markedAsTakenCalled = true }, // Lambda that will be called by internalOnMarkAsTaken
                    onDismiss = {}
                )
            }
        }

        // Click "Mark as Taken" button
        composeTestRule.onNodeWithText(markAsTakenText, useUnmergedTree = true).performClick()

        // Verify onMarkAsTaken lambda was eventually called
        assert(markedAsTakenCalled) { "onMarkAsTaken should have been called" }
        
        // After click, the internal state 'showTick' becomes true.
        // Verify texts and buttons are hidden
        composeTestRule.onNodeWithText(medicationName).assertDoesNotExist()
        composeTestRule.onNodeWithText(medicationDosage).assertDoesNotExist()
        composeTestRule.onNodeWithText(markAsTakenText, useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText(dismissText, useUnmergedTree = true).assertDoesNotExist()

        // Verify tick is shown (using its content description)
        composeTestRule.onNodeWithContentDescription(tickContentDescription, useUnmergedTree = true).assertIsDisplayed()

        // Verify the original medication icon is no longer displayed
        composeTestRule.onNodeWithContentDescription(pillIconContentDescription, useUnmergedTree = true).assertDoesNotExist()
    }
}
