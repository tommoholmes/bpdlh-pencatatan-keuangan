package com.bpdlh.pencatatankeuangan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

const val SYNC_PENDING = "pending"
const val SYNC_SYNCED = "synced"
const val SYNC_FAILED = "failed"

@Entity(tableName = "records")
data class Record(
    @PrimaryKey val id: String,
    val name: String,
    val price: Long,
    val photoId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyM: Double?,
    val deviceId: String?,
    val deviceModel: String?,
    val osVersion: String?,
    val appVersion: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val seq: Long,
    val syncStatus: String,
    val photoPath: String?,
)

@Entity(tableName = "sync_state")
data class SyncState(
    @PrimaryKey val id: Int = 0,
    val lastPullSeq: Long,
    val deviceId: String,
)
