package com.bbks.mydailytracker

enum class SortOption(val displayName: String) {
    ALPHABETICAL("가나다순"),
    RECENT("최근 추가순"),
    COMPLETED_FIRST("완료 우선"),
    MANUAL("사용자 지정")
}