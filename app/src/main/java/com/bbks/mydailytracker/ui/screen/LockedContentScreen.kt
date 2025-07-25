package com.bbks.mydailytracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.bbks.mydailytracker.R
import java.util.Locale

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
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(16.dp))
                .background(cardColor)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 타이틀
            Text(
                text = stringResource(R.string.premium_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 설명 텍스트
            Text(
                text = stringResource(R.string.premium_description),
                style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 혜택 목록
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "📊 " + stringResource(R.string.premium_benefit_stats),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🚫 " + stringResource(R.string.premium_benefit_no_ads),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚙️ " + stringResource(R.string.premium_benefit_unlimited_detail),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val locale = Locale.getDefault().language
            val imageRes = when (locale) {
                "ko" -> R.drawable.stats_kr
                "ja" -> R.drawable.stats_ja
                else -> R.drawable.stats
            }

            // 통계 미리보기 이미지 (카드 스타일)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFF1CD))
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = stringResource(R.string.premium_preview_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onUpgradeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp), // 여유있는 가로 여백 추가
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.premium_upgrade_button),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.back_button_description),
                tint = textColor
            )
        }
    }
}