package com.bpdlh.pencatatankeuangan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [Record::class, SyncState::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun records(): RecordDao
    abstract fun syncState(): SyncStateDao
}

object DatabaseFactory {
    /**
     * Database lokal terenkripsi SQLCipher. Kunci acak dibuat saat pertama dipakai dan
     * disimpan terbungkus Android Keystore (lihat [DatabaseKey]).
     */
    fun open(context: Context, passphrase: ByteArray): AppDatabase {
        // sqlcipher-android tidak memuat pustaka aslinya sendiri; tanpa baris ini open() melempar UnsatisfiedLinkError.
        System.loadLibrary("sqlcipher")
        return Room.databaseBuilder(context, AppDatabase::class.java, "pencatatan.db")
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
    }
}
