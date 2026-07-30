package com.brixavier.tskr.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.brixavier.tskr.model.Reminder
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for Room database operations on Reminders.
 */
@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY date ASC, time ASC")
    fun getAllRemindersFlow(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: String): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Update
    suspend fun update(reminder: Reminder)

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY date ASC, time ASC")
    suspend fun getUpcomingReminders(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY date ASC, time ASC LIMIT :limit")
    suspend fun getUpcomingRemindersLimit(limit: Int): List<Reminder>

    @Delete
    suspend fun deleteReminder(reminder: Reminder)
}
