package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "test_reports")
data class TestReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val passedCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
    val totalCount: Int,
    val resultsString: String, // format: "test_id:STATUS;test_id:STATUS"
    val deviceModel: String,
    val androidVersion: String
)

@Dao
interface TestReportDao {
    @Query("SELECT * FROM test_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<TestReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: TestReport)

    @Query("DELETE FROM test_reports WHERE id = :id")
    suspend fun deleteReportById(id: Int)

    @Query("DELETE FROM test_reports")
    suspend fun deleteAllReports()
}

@Database(entities = [TestReport::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun testReportDao(): TestReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "phone_tester_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class TestReportRepository(private val testReportDao: TestReportDao) {
    val allReports: Flow<List<TestReport>> = testReportDao.getAllReports()

    suspend fun insert(report: TestReport) {
        testReportDao.insertReport(report)
    }

    suspend fun deleteById(id: Int) {
        testReportDao.deleteReportById(id)
    }

    suspend fun deleteAll() {
        testReportDao.deleteAllReports()
    }
}
