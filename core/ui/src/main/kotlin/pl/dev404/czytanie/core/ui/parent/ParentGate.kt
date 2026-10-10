package pl.dev404.czytanie.core.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.dev404.czytanie.core.domain.parent.ParentGateChallenge
import pl.dev404.czytanie.core.ui.R

const val PARENT_GATE_TAG = "parent_gate"

/**
 * Shows [content] only after an adult has passed the parental gate. The gate stays open while
 * the screen lives (also across rotation); leaving the screen closes it again.
 */
@Composable
fun ParentGated(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    newChallenge: () -> ParentGateChallenge = { ParentGateChallenge.random() },
    content: @Composable () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (unlocked) {
        content()
    } else {
        ParentGate(onUnlocked = {
            unlocked = true
        }, onCancel = onCancel, newChallenge = newChallenge, modifier = modifier)
    }
}

/**
 * A multiplication answered on a number pad: easy for an adult, out of reach for a child who is
 * learning to read. A wrong answer brings a new task, so guessing does not get through.
 */
@Composable
fun ParentGate(
    onUnlocked: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    newChallenge: () -> ParentGateChallenge = { ParentGateChallenge.random() },
) {
    val first = remember { newChallenge() }
    var left by rememberSaveable { mutableIntStateOf(first.left) }
    var right by rememberSaveable { mutableIntStateOf(first.right) }
    var input by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize().testTag(PARENT_GATE_TAG)) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.parent_gate_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.parent_gate_question, left, right),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = input.ifEmpty { "–" },
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            if (wrong) {
                Text(
                    text = stringResource(R.string.parent_gate_wrong),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
            }
            NumberPad(
                onDigit = { digit ->
                    if (input.length < MAX_DIGITS) input += digit
                    wrong = false
                },
                onDelete = { input = input.dropLast(1) },
                onConfirm = {
                    if (ParentGateChallenge(left, right).isCorrect(input)) {
                        onUnlocked()
                    } else {
                        wrong = true
                        input = ""
                        newChallenge().let {
                            left = it.left
                            right = it.right
                        }
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onCancel) { Text(stringResource(R.string.parent_gate_cancel)) }
        }
    }
}

@Composable
private fun NumberPad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
) {
    val deleteDescription = stringResource(R.string.parent_gate_delete)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.widthIn(max = 320.dp)) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit -> PadKey(digit.toString()) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadKey("⌫", Modifier.semantics { contentDescription = deleteDescription }, onDelete)
            PadKey("0") { onDigit('0') }
            PadKey(stringResource(R.string.parent_gate_confirm), onClick = onConfirm)
        }
    }
}

@Composable
private fun PadKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilledTonalButton(onClick = onClick, modifier = modifier.size(width = 88.dp, height = 64.dp)) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

private const val MAX_DIGITS = 3
