package com.bbks.mydailytracker

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdMobBanner() {
    AndroidView(factory = { context ->
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = "ca-app-pub-7350776421233026/7855766646"
            loadAd(AdRequest.Builder().build())
        }
    }, update = {
        it.loadAd(AdRequest.Builder().build())
    })
}
