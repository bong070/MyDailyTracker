package com.bbks.mydailytracker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import SortOption
import java.time.LocalTime


val Context.datastore by preferencesDataStore(name = "user_preferences")

private const val DATASTORE_NAME = "user_prefs"
val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class SettingsRepository(private val context: Context) {

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            endHour = prefs[PreferenceKeys.END_HOUR] ?: 23,
            endMinute = prefs[PreferenceKeys.END_MINUTE] ?: 59,
            alarmEnabled = prefs[PreferenceKeys.ALARM_ENABLED] ?: false,
            autoDelete = prefs[PreferenceKeys.AUTO_DELETE] ?: false,
            sortOption = SortOption.valueOf(prefs[PreferenceKeys.SORT_OPTION] ?: SortOption.ALPHABETICAL.name)
        )
    }

    suspend fun savePreferences(prefs: UserPreferences) {
        context.dataStore.edit { settings ->
            settings[PreferenceKeys.END_HOUR] = prefs.endHour
            settings[PreferenceKeys.END_MINUTE] = prefs.endMinute
            settings[PreferenceKeys.ALARM_ENABLED] = prefs.alarmEnabled
            settings[PreferenceKeys.AUTO_DELETE] = prefs.autoDelete
            settings[PreferenceKeys.SORT_OPTION] = prefs.sortOption.name
        }
    }

    suspend fun updateAlarmEnabled(enabled: Boolean) {
        context.datastore.edit { preferences ->
            preferences[PreferenceKeys.ALARM_ENABLED] = enabled
        }
    }

    suspend fun updateAutoDelete(enabled: Boolean) {
        context.datastore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_DELETE] = enabled
        }
    }

    suspend fun updateSortOption(option: SortOption) {
        context.datastore.edit { preferences ->
            preferences[PreferenceKeys.SORT_OPTION] = option.name
        }
    }

    suspend fun updateEndTime(time: LocalTime) {
        context.datastore.edit { preferences ->
            preferences[PreferenceKeys.END_HOUR] = time.hour
            preferences[PreferenceKeys.END_MINUTE] = time.minute
        }
    }
}
