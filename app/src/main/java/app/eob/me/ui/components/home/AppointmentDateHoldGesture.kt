package app.eob.me.ui.components.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val APPOINTMENT_DATE_HOLD_MS = 2_000L

fun Modifier.appointmentDateHoldClickable(
    onHoldComplete: () -> Unit,
    holdDurationMs: Long = APPOINTMENT_DATE_HOLD_MS
): Modifier = pointerInput(holdDurationMs, onHoldComplete) {
    detectTapGestures(
        onPress = {
            coroutineScope {
                val holdJob = launch {
                    delay(holdDurationMs)
                    onHoldComplete()
                }
                tryAwaitRelease()
                holdJob.cancel()
            }
        }
    )
}
