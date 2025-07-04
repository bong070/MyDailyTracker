package com.bbks.mydailytracker

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private const val DATASTORE_NAME = "user_prefs"
val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class SettingsRepository(private val context: Context) {

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            endHour = prefs[PreferenceKeys.END_HOUR] ?: 23,
            endMinute = prefs[PreferenceKeys.END_MINUTE] ?: 59,
            alarmEnabled = prefs[PreferenceKeys.ALARM_ENABLED] ?: false,
            autoDelete = prefs[PreferenceKeys.AUTO_DELETE] ?: false,
            sortOption = SortOption.valueOf(prefs[PreferenceKeys.SORT_OPTION] ?: SortOption.ALPHABETICAL.name),
            isPremiumUser = prefs[PreferenceKeys.IS_PREMIUM_USER] ?: false
        )
    }

    suspend fun updateAlarmEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.ALARM_ENABLED] = enabled
        }
    }

    suspend fun updateAutoDelete(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_DELETE] = enabled
        }
    }

    suspend fun updateSortOption(option: SortOption) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.SORT_OPTION] = option.name
        }
    }

    val premiumUserFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.IS_PREMIUM_USER] ?: false
        }

    suspend fun setPremiumUser(isPremium: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_PREMIUM_USER] = isPremium
        }
    }
}
