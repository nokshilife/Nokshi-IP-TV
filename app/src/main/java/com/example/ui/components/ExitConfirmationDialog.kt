package com.example.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ExitConfirmationDialog(
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    val stayFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            stayFocusRequester.requestFocus()
        } catch (e: Exception) {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color(0xFF1B2623),
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Exit App",
                tint = Color(0xFFB8CFC9)
            )
        },
        title = {
            Text(
                text = "অ্যাপ থেকে প্রস্থান",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = "আপনি কি Nokshi IP TV অ্যাপ থেকে বের হতে চান?",
                color = Color(0xFFB0BEC5),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TvNeutralButton(
                onClick = onConfirmExit,
                modifier = Modifier.testTag("exit_confirm_button")
            ) {
                Text("হ্যাঁ, বের হন")
            }
        },
        dismissButton = {
            TvNeutralButton(
                onClick = onDismiss,
                focusRequester = stayFocusRequester,
                modifier = Modifier.testTag("exit_cancel_button")
            ) {
                Text("না, অ্যাপে থাকুন")
            }
        }
    )
}
