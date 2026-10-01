package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.MahiPersona
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.TextPrimary

@Composable
fun QuickActionChips(
    onChipSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("quick_action_chips_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MahiPersona.QUICK_PROMPTS.forEachIndexed { index, (label, prompt) ->
            val isEven = index % 2 == 0
            val borderAccent = if (isEven) NeonPink.copy(alpha = 0.5f) else NeonCyan.copy(alpha = 0.5f)

            Surface(
                onClick = { onChipSelected(prompt) },
                shape = RoundedCornerShape(20.dp),
                color = DarkSurfaceElevated.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, borderAccent),
                modifier = Modifier.testTag("chip_${label.filter { it.isLetter() }}")
            ) {
                Text(
                    text = label,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
