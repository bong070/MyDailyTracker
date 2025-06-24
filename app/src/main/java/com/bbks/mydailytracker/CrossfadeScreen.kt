package com.bbks.mydailytracker.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable

@Composable
fun <T> CrossfadeScreen(targetState: T, content: @Composable () -> Unit) {
    Crossfade(targetState = targetState) {
        content()
    }
}