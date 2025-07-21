package com.bbks.mydailytracker.util

import androidx.annotation.StringRes
import com.bbks.mydailytracker.R

enum class SortOption(@StringRes val labelResId: Int) {
    ALPHABETICAL(R.string.sort_alphabetical),
    RECENT(R.string.sort_recent),
    INCOMPLETED_FIRST(R.string.sort_incompleted_first),
    MANUAL(R.string.sort_manual)
}