package com.samind.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trigger_events")
data class TriggerEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sourcePackage: String,
    val score: Float,
)
