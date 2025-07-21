package com.bbks.mydailytracker.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdMobBanner() {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                //adUnitId = "ca-app-pub-3940256099942544/6300978111" // 테스트용
                adUnitId = "ca-app-pub-2864557421723275/5509750421"
                loadAd(AdRequest.Builder().build())
            }
        },
        update = {
            it.loadAd(AdRequest.Builder().build())
        }
    )
}
