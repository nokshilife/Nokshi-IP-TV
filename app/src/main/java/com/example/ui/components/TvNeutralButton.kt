package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Universal TV & Mobile button that stays calm and neutral when NOT focused/selected,
 * and highlights distinctly ONLY when focused (D-pad) or actively pressed (touch).
 */
@Composable
fun TvNeutralButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHighlighted = enabled && (isFocused || isPressed)

    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.05f else 1.0f,
        label = "neutral_btn_scale"
    )

    val containerColor = when {
        !enabled -> Color(0xFF18211F)
        isHighlighted -> Color(0xFFFF9800) // Highlighted ONLY when focused/pressed
        else -> Color(0xFF22302C)          // Calm neutral dark slate when unfocused
    }

    val borderColor = when {
        !enabled -> Color(0xFF263330)
        isHighlighted -> Color(0xFFFFE082)
        else -> Color(0xFF3E544E)
    }

    val contentColor = when {
        !enabled -> Color(0xFF5D746E)
        isHighlighted -> Color.Black
        else -> Color(0xFFE4F0EC)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(
            width = if (isHighlighted) 2.5.dp else 1.dp,
            color = borderColor
        ),
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minWidth = 64.dp, minHeight = 42.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(
                value = TextStyle(
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(contentPadding),
                    content = content
                )
            }
        }
    }
}
