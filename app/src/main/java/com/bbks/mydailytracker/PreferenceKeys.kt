package com.bbks.mydailytracker

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceKeys {
    val END_HOUR = intPreferencesKey("end_hour")
    val END_MINUTE = intPreferencesKey("end_minute")
    val ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
    val AUTO_DELETE = booleanPreferencesKey("auto_delete")
    val SORT_OPTION = stringPreferencesKey("sort_option")
    val IS_PREMIUM_USER = androidx.datastore.preferences.core.booleanPreferencesKey("is_premium_user")
}
