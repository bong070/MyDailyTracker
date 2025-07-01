package com.bbks.mydailytracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.draw.clip

@Composable
fun LockedContentScreen(
    onUpgradeClick: () -> Unit,
    onBack: () -> Unit
) {
    val backgroundColor = Color(0xFFFFF8E1)
    val cardColor = Color(0xFFF7EBD5)
    val lockIconColor = Color(0xFFFF7043)
    val textColor = Color(0xFF212121)
    val buttonColor = Color(0xFFFF7043)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(cardColor)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "프리미엄 기능입니다",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "해당 기능은 프리미엄 사용자만 사용할 수 있어요.",
                style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock Icon",
                modifier = Modifier.size(72.dp),
                tint = lockIconColor
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onUpgradeClick,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("업그레이드", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
        }
    }
}

