package com.bpdlh.pencatatankeuangan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM records WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<Record>>

    @Query("SELECT COUNT(*) FROM records WHERE syncStatus = 'pending'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM records WHERE id = :id")
    fun observeById(id: String): Flow<Record?>

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun findById(id: String): Record?

    @Query("SELECT COUNT(*) FROM records WHERE syncStatus = 'pending'")
    suspend fun countPending(): Int
    @Query("SELECT * FROM records WHERE syncStatus = 'pending' ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun pending(limit: Int): List<Record>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: Record)

    @Query("UPDATE records SET syncStatus = :status WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markStatusIfUnchanged(id: String, updatedAt: Long, status: String)

    @Query("UPDATE records SET photoPath = :path WHERE id = :id")
    suspend fun setPhotoPath(id: String, path: String?)

    @Transaction
    suspend fun applyRemote(rows: List<Record>) {
        for (row in rows) {
            if (findById(row.id)?.syncStatus == SYNC_PENDING) continue
            upsert(row)
        }
    }

    @Query("DELETE FROM records")
    suspend fun clearRecords()
}

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE id = 0")
    fun observe(): Flow<SyncState?>

    @Query("SELECT * FROM sync_state WHERE id = 0")
    suspend fun get(): SyncState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(state: SyncState)
}
