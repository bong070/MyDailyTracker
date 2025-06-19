package com.bbks.mydailytracker

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
                adUnitId = "ca-app-pub-7350776421233026/7855766646"
                loadAd(AdRequest.Builder().build())
            }
        },
        update = {
            it.loadAd(AdRequest.Builder().build())
        }
    )
}
