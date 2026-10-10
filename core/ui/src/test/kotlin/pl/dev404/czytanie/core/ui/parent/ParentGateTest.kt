package pl.dev404.czytanie.core.ui.parent

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pl.dev404.czytanie.core.domain.parent.ParentGateChallenge
import pl.dev404.czytanie.core.ui.R
import pl.dev404.czytanie.core.ui.theme.CzytanieTheme

/** A child tapping around must not get into the parent's area; an adult gets in at once. */
@RunWith(RobolectricTestRunner::class)
class ParentGateTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var challengesShown = 0

    /** 7 × 8 first, then 6 × 9 for every new task. */
    private fun nextChallenge() = if (challengesShown++ == 0) ParentGateChallenge(7, 8) else ParentGateChallenge(6, 9)

    private fun showGate() {
        composeRule.setContent {
            CzytanieTheme {
                ParentGated(onCancel = {}, newChallenge = ::nextChallenge) { Text(SECRET) }
            }
        }
    }

    private fun tap(keys: String) {
        keys.forEach { composeRule.onNodeWithText(it.toString()).performClick() }
        composeRule.onNodeWithText(context.getString(R.string.parent_gate_confirm)).performClick()
    }

    @Test
    fun correctAnswerOpensTheGate() {
        showGate()
        composeRule.onNodeWithText("Ile to 7 × 8?").assertExists()

        tap("56")

        composeRule.onNodeWithText(SECRET).assertExists()
    }

    @Test
    fun randomTappingKeepsTheGateClosedAndChangesTheTask() {
        showGate()

        tap("1")
        tap("")

        composeRule.onNodeWithText(SECRET).assertDoesNotExist()
        composeRule.onNodeWithTag(PARENT_GATE_TAG).assertExists()
        // The old answer does not work for the new task.
        composeRule.onNodeWithText("Ile to 6 × 9?").assertExists()
        tap("56")
        composeRule.onNodeWithText(SECRET).assertDoesNotExist()
    }

    @Test
    fun deleteRemovesTheLastDigit() {
        showGate()
        composeRule.onNodeWithText("5").performClick()
        composeRule.onNodeWithText("9").performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.parent_gate_delete)).performClick()

        tap("6")

        composeRule.onNodeWithText(SECRET).assertExists()
    }

    private companion object {
        const val SECRET = "panel rodzica"
    }
}
