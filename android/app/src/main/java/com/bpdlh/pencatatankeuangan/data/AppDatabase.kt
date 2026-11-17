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

    fun open(context: Context, passphrase: ByteArray): AppDatabase {
        System.loadLibrary("sqlcipher")
        return Room.databaseBuilder(context, AppDatabase::class.java, "pencatatan.db")
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
    }
}
