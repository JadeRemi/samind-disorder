package com.samind.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TriggerEventDao {

    @Insert
    suspend fun insert(event: TriggerEvent)

    @Query("SELECT COUNT(*) FROM trigger_events")
    fun totalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM trigger_events WHERE timestamp >= :since")
    fun countSince(since: Long): Flow<Int>

    @Query("SELECT * FROM trigger_events ORDER BY timestamp DESC LIMIT :limit")
    fun recent(limit: Int = 20): Flow<List<TriggerEvent>>
}
