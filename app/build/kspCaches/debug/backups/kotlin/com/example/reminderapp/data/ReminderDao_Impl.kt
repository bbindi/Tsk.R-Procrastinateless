package com.example.reminderapp.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.reminderapp.model.Reminder
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ReminderDao_Impl(
  __db: RoomDatabase,
) : ReminderDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfReminder: EntityInsertAdapter<Reminder>

  private val __deleteAdapterOfReminder: EntityDeleteOrUpdateAdapter<Reminder>

  private val __updateAdapterOfReminder: EntityDeleteOrUpdateAdapter<Reminder>
  init {
    this.__db = __db
    this.__insertAdapterOfReminder = object : EntityInsertAdapter<Reminder>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `reminders` (`id`,`title`,`date`,`time`,`isActive`,`priority`,`notes`,`subTasksRaw`,`motivation`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Reminder) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.date)
        statement.bindText(4, entity.time)
        val _tmp: Int = if (entity.isActive) 1 else 0
        statement.bindLong(5, _tmp.toLong())
        statement.bindText(6, entity.priority)
        statement.bindText(7, entity.notes)
        statement.bindText(8, entity.subTasksRaw)
        statement.bindText(9, entity.motivation)
      }
    }
    this.__deleteAdapterOfReminder = object : EntityDeleteOrUpdateAdapter<Reminder>() {
      protected override fun createQuery(): String = "DELETE FROM `reminders` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Reminder) {
        statement.bindText(1, entity.id)
      }
    }
    this.__updateAdapterOfReminder = object : EntityDeleteOrUpdateAdapter<Reminder>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `reminders` SET `id` = ?,`title` = ?,`date` = ?,`time` = ?,`isActive` = ?,`priority` = ?,`notes` = ?,`subTasksRaw` = ?,`motivation` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Reminder) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.date)
        statement.bindText(4, entity.time)
        val _tmp: Int = if (entity.isActive) 1 else 0
        statement.bindLong(5, _tmp.toLong())
        statement.bindText(6, entity.priority)
        statement.bindText(7, entity.notes)
        statement.bindText(8, entity.subTasksRaw)
        statement.bindText(9, entity.motivation)
        statement.bindText(10, entity.id)
      }
    }
  }

  public override suspend fun insertReminder(reminder: Reminder): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfReminder.insertAndReturnId(_connection, reminder)
    _result
  }

  public override suspend fun deleteReminder(reminder: Reminder): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfReminder.handle(_connection, reminder)
  }

  public override suspend fun updateReminder(reminder: Reminder): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfReminder.handle(_connection, reminder)
  }

  public override suspend fun update(reminder: Reminder): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfReminder.handle(_connection, reminder)
  }

  public override fun getAllRemindersFlow(): Flow<List<Reminder>> {
    val _sql: String = "SELECT * FROM reminders ORDER BY date ASC, time ASC"
    return createFlow(__db, false, arrayOf("reminders")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfTime: Int = getColumnIndexOrThrow(_stmt, "time")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "isActive")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfSubTasksRaw: Int = getColumnIndexOrThrow(_stmt, "subTasksRaw")
        val _columnIndexOfMotivation: Int = getColumnIndexOrThrow(_stmt, "motivation")
        val _result: MutableList<Reminder> = mutableListOf()
        while (_stmt.step()) {
          val _item: Reminder
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpTime: String
          _tmpTime = _stmt.getText(_columnIndexOfTime)
          val _tmpIsActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp != 0
          val _tmpPriority: String
          _tmpPriority = _stmt.getText(_columnIndexOfPriority)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpSubTasksRaw: String
          _tmpSubTasksRaw = _stmt.getText(_columnIndexOfSubTasksRaw)
          val _tmpMotivation: String
          _tmpMotivation = _stmt.getText(_columnIndexOfMotivation)
          _item = Reminder(_tmpId,_tmpTitle,_tmpDate,_tmpTime,_tmpIsActive,_tmpPriority,_tmpNotes,_tmpSubTasksRaw,_tmpMotivation)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getReminderById(id: String): Reminder? {
    val _sql: String = "SELECT * FROM reminders WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfTime: Int = getColumnIndexOrThrow(_stmt, "time")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "isActive")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfSubTasksRaw: Int = getColumnIndexOrThrow(_stmt, "subTasksRaw")
        val _columnIndexOfMotivation: Int = getColumnIndexOrThrow(_stmt, "motivation")
        val _result: Reminder?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpTime: String
          _tmpTime = _stmt.getText(_columnIndexOfTime)
          val _tmpIsActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp != 0
          val _tmpPriority: String
          _tmpPriority = _stmt.getText(_columnIndexOfPriority)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpSubTasksRaw: String
          _tmpSubTasksRaw = _stmt.getText(_columnIndexOfSubTasksRaw)
          val _tmpMotivation: String
          _tmpMotivation = _stmt.getText(_columnIndexOfMotivation)
          _result = Reminder(_tmpId,_tmpTitle,_tmpDate,_tmpTime,_tmpIsActive,_tmpPriority,_tmpNotes,_tmpSubTasksRaw,_tmpMotivation)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getUpcomingReminders(): List<Reminder> {
    val _sql: String = "SELECT * FROM reminders WHERE isActive = 1 ORDER BY date ASC, time ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfTime: Int = getColumnIndexOrThrow(_stmt, "time")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "isActive")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfSubTasksRaw: Int = getColumnIndexOrThrow(_stmt, "subTasksRaw")
        val _columnIndexOfMotivation: Int = getColumnIndexOrThrow(_stmt, "motivation")
        val _result: MutableList<Reminder> = mutableListOf()
        while (_stmt.step()) {
          val _item: Reminder
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpTime: String
          _tmpTime = _stmt.getText(_columnIndexOfTime)
          val _tmpIsActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp != 0
          val _tmpPriority: String
          _tmpPriority = _stmt.getText(_columnIndexOfPriority)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpSubTasksRaw: String
          _tmpSubTasksRaw = _stmt.getText(_columnIndexOfSubTasksRaw)
          val _tmpMotivation: String
          _tmpMotivation = _stmt.getText(_columnIndexOfMotivation)
          _item = Reminder(_tmpId,_tmpTitle,_tmpDate,_tmpTime,_tmpIsActive,_tmpPriority,_tmpNotes,_tmpSubTasksRaw,_tmpMotivation)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getUpcomingRemindersLimit(limit: Int): List<Reminder> {
    val _sql: String = "SELECT * FROM reminders WHERE isActive = 1 ORDER BY date ASC, time ASC LIMIT ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfTime: Int = getColumnIndexOrThrow(_stmt, "time")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "isActive")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfSubTasksRaw: Int = getColumnIndexOrThrow(_stmt, "subTasksRaw")
        val _columnIndexOfMotivation: Int = getColumnIndexOrThrow(_stmt, "motivation")
        val _result: MutableList<Reminder> = mutableListOf()
        while (_stmt.step()) {
          val _item: Reminder
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpTime: String
          _tmpTime = _stmt.getText(_columnIndexOfTime)
          val _tmpIsActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp != 0
          val _tmpPriority: String
          _tmpPriority = _stmt.getText(_columnIndexOfPriority)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpSubTasksRaw: String
          _tmpSubTasksRaw = _stmt.getText(_columnIndexOfSubTasksRaw)
          val _tmpMotivation: String
          _tmpMotivation = _stmt.getText(_columnIndexOfMotivation)
          _item = Reminder(_tmpId,_tmpTitle,_tmpDate,_tmpTime,_tmpIsActive,_tmpPriority,_tmpNotes,_tmpSubTasksRaw,_tmpMotivation)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
