package com.bbks.mydailytracker.alarm

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.oneShotDataStore by preferencesDataStore(name = "oneshot_store")

object OneShotStore {
    private fun key(habitId: Int) = longPreferencesKey("oneshot_at_$habitId")

    suspend fun set(context: Context, habitId: Int, atMillis: Long) {
        context.oneShotDataStore.edit { it[key(habitId)] = atMillis }
    }

    suspend fun get(context: Context, habitId: Int): Long? {
        val prefs = context.oneShotDataStore.data.first()
        return prefs[key(habitId)]
    }

    suspend fun clear(context: Context, habitId: Int) {
        context.oneShotDataStore.edit { it.remove(key(habitId)) }
    }
}
