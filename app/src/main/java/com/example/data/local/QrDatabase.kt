package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.QrHistoryType
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "qr_history")
data class QrHistoryEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val type: QrHistoryType,
    val errorCorrection: String = "M",
    val createdAt: Long = System.currentTimeMillis()
)

class QrHistoryConverters {
    @TypeConverter
    fun fromType(value: QrHistoryType): String = value.name

    @TypeConverter
    fun toType(value: String): QrHistoryType =
        QrHistoryType.entries.firstOrNull { it.name == value } ?: QrHistoryType.GENERATED
}

@Dao
interface QrHistoryDao {
    @Query("SELECT * FROM qr_history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<QrHistoryEntity>>

    @Query("SELECT * FROM qr_history ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): QrHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: QrHistoryEntity): Long

    @Query("DELETE FROM qr_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM qr_history")
    suspend fun clearAll()
}

@Database(entities = [QrHistoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(QrHistoryConverters::class)
abstract class QrDatabase : RoomDatabase() {
    abstract fun qrHistoryDao(): QrHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: QrDatabase? = null

        fun getInstance(context: Context): QrDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QrDatabase::class.java,
                    "qrcode_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
