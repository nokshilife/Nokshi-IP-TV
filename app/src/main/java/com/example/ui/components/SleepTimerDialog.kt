package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepTimerDialog(
    isActive: Boolean,
    remainingSeconds: Int,
    onSetTimer: (minutes: Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf(15, 30, 45, 60, 90, 120)

    val formattedRemaining = if (remainingSeconds > 0) {
        val min = remainingSeconds / 60
        val sec = remainingSeconds % 60
        String.format("%02d:%02d", min, sec)
    } else ""

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color(0xFF1B2623),
        icon = {
            Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = "Sleep Timer",
                tint = Color(0xFFB8CFC9)
            )
        },
        title = {
            Text(
                text = "স্লিপ টাইমার",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isActive) {
                    Text(
                        text = "টাইমার সক্রিয় রয়েছে",
                        color = Color(0xFF80CBC4),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "অ্যাপ বন্ধ হতে বাকি: $formattedRemaining",
                        color = Color(0xFF76FF03),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Text(
                        text = "নির্দিষ্ট সময় পর অ্যাপটি স্বয়ংক্রিয়ভাবে বন্ধ হয়ে যাবে।",
                        color = Color(0xFFB0BEC5),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "সময় নির্বাচন করুন:",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.forEach { min ->
                        TvNeutralButton(
                            onClick = {
                                onSetTimer(min)
                                onDismiss()
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Text("$min মিনিট")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isActive) {
                TvNeutralButton(
                    onClick = {
                        onCancelTimer()
                        onDismiss()
                    }
                ) {
                    Text("টাইমার বাতিল করুন")
                }
            }
        },
        dismissButton = {
            TvNeutralButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}
