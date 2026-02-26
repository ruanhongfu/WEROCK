package com.werock.demo.bg

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bgStatusDataStore: DataStore<Preferences> by preferencesDataStore(name = "bg_status")

object BgStatusStore {
    private val lastUpdatedKey = longPreferencesKey("last_updated_ms")

    fun lastUpdatedFlow(context: Context): Flow<Long> {
        return context.bgStatusDataStore.data.map { it[lastUpdatedKey] ?: 0L }
    }

    suspend fun setLastUpdated(context: Context, timeMillis: Long) {
        context.bgStatusDataStore.edit { preferences ->
            preferences[lastUpdatedKey] = timeMillis
        }
    }
}

