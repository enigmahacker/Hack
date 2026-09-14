package com.example.jarvis.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.jarvis.data.model.ChatMessage
import com.example.jarvis.data.model.JarvisTask
import com.example.jarvis.data.model.MemoryCategory
import com.example.jarvis.data.model.MemoryItem
import com.example.jarvis.data.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JarvisDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "jarvis_assistant.db"
        const val DATABASE_VERSION = 1

        // Table: memories
        const val TABLE_MEMORIES = "memories"
        const val COL_MEM_ID = "id"
        const val COL_MEM_CATEGORY = "category"
        const val COL_MEM_KEY = "mem_key"
        const val COL_MEM_VALUE = "mem_value"
        const val COL_MEM_TIMESTAMP = "timestamp"

        // Table: messages
        const val TABLE_MESSAGES = "messages"
        const val COL_MSG_ID = "id"
        const val COL_MSG_ROLE = "role"
        const val COL_MSG_CONTENT = "content"
        const val COL_MSG_TOOL_NAME = "tool_name"
        const val COL_MSG_TOOL_RESULT = "tool_result"
        const val COL_MSG_TIMESTAMP = "timestamp"

        // Table: tasks
        const val TABLE_TASKS = "tasks"
        const val COL_TASK_ID = "id"
        const val COL_TASK_TITLE = "title"
        const val COL_TASK_NOTES = "notes"
        const val COL_TASK_DUE = "due_timestamp"
        const val COL_TASK_COMPLETED = "is_completed"
        const val COL_TASK_PRIORITY = "priority"
        const val COL_TASK_CREATED = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_MEMORIES (
                $COL_MEM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_MEM_CATEGORY TEXT NOT NULL,
                $COL_MEM_KEY TEXT NOT NULL,
                $COL_MEM_VALUE TEXT NOT NULL,
                $COL_MEM_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_MESSAGES (
                $COL_MSG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_MSG_ROLE TEXT NOT NULL,
                $COL_MSG_CONTENT TEXT NOT NULL,
                $COL_MSG_TOOL_NAME TEXT,
                $COL_MSG_TOOL_RESULT TEXT,
                $COL_MSG_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_TASKS (
                $COL_TASK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TASK_TITLE TEXT NOT NULL,
                $COL_TASK_NOTES TEXT,
                $COL_TASK_DUE INTEGER,
                $COL_TASK_COMPLETED INTEGER NOT NULL DEFAULT 0,
                $COL_TASK_PRIORITY INTEGER NOT NULL DEFAULT 1,
                $COL_TASK_CREATED INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration logic for future schema increments
    }

    // --- Memories Operations ---
    suspend fun insertMemory(item: MemoryItem): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_MEM_CATEGORY, item.category.name)
            put(COL_MEM_KEY, item.key)
            put(COL_MEM_VALUE, item.value)
            put(COL_MEM_TIMESTAMP, item.timestamp)
        }
        db.insertWithOnConflict(TABLE_MEMORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun getAllMemories(): List<MemoryItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MemoryItem>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_MEMORIES, null, null, null, null, null, "$COL_MEM_TIMESTAMP DESC"
        )
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_MEM_ID)
            val catIdx = it.getColumnIndexOrThrow(COL_MEM_CATEGORY)
            val keyIdx = it.getColumnIndexOrThrow(COL_MEM_KEY)
            val valIdx = it.getColumnIndexOrThrow(COL_MEM_VALUE)
            val tsIdx = it.getColumnIndexOrThrow(COL_MEM_TIMESTAMP)
            while (it.moveToNext()) {
                val catStr = it.getString(catIdx)
                val cat = try {
                    MemoryCategory.valueOf(catStr)
                } catch (e: Exception) {
                    MemoryCategory.FACT
                }
                list.add(
                    MemoryItem(
                        id = it.getLong(idIdx),
                        category = cat,
                        key = it.getString(keyIdx),
                        value = it.getString(valIdx),
                        timestamp = it.getLong(tsIdx)
                    )
                )
            }
        }
        list
    }

    suspend fun deleteMemory(id: Long): Int = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_MEMORIES, "$COL_MEM_ID = ?", arrayOf(id.toString()))
    }

    suspend fun clearAllMemories(): Int = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_MEMORIES, null, null)
    }

    // --- Messages Operations ---
    suspend fun insertMessage(message: ChatMessage): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COL_MSG_ROLE, message.role.name)
            put(COL_MSG_CONTENT, message.content)
            put(COL_MSG_TOOL_NAME, message.toolName)
            put(COL_MSG_TOOL_RESULT, message.toolResult)
            put(COL_MSG_TIMESTAMP, message.timestamp)
        }
        writableDatabase.insert(TABLE_MESSAGES, null, values)
    }

    suspend fun getRecentMessages(limit: Int = 50): List<ChatMessage> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ChatMessage>()
        val cursor = readableDatabase.query(
            TABLE_MESSAGES, null, null, null, null, null, "$COL_MSG_TIMESTAMP ASC", limit.toString()
        )
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_MSG_ID)
            val roleIdx = it.getColumnIndexOrThrow(COL_MSG_ROLE)
            val contentIdx = it.getColumnIndexOrThrow(COL_MSG_CONTENT)
            val toolNameIdx = it.getColumnIndexOrThrow(COL_MSG_TOOL_NAME)
            val toolResIdx = it.getColumnIndexOrThrow(COL_MSG_TOOL_RESULT)
            val tsIdx = it.getColumnIndexOrThrow(COL_MSG_TIMESTAMP)
            while (it.moveToNext()) {
                val role = try {
                    MessageRole.valueOf(it.getString(roleIdx))
                } catch (e: Exception) {
                    MessageRole.USER
                }
                list.add(
                    ChatMessage(
                        id = it.getLong(idIdx),
                        role = role,
                        content = it.getString(contentIdx),
                        toolName = it.getString(toolNameIdx),
                        toolResult = it.getString(toolResIdx),
                        timestamp = it.getLong(tsIdx)
                    )
                )
            }
        }
        list
    }

    suspend fun clearMessages(): Int = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_MESSAGES, null, null)
    }

    // --- Tasks Operations ---
    suspend fun insertTask(task: JarvisTask): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COL_TASK_TITLE, task.title)
            put(COL_TASK_NOTES, task.notes)
            put(COL_TASK_DUE, task.dueTimestamp)
            put(COL_TASK_COMPLETED, if (task.isCompleted) 1 else 0)
            put(COL_TASK_PRIORITY, task.priority)
            put(COL_TASK_CREATED, task.createdAt)
        }
        writableDatabase.insert(TABLE_TASKS, null, values)
    }

    suspend fun getAllTasks(): List<JarvisTask> = withContext(Dispatchers.IO) {
        val list = mutableListOf<JarvisTask>()
        val cursor = readableDatabase.query(
            TABLE_TASKS, null, null, null, null, null, "$COL_TASK_COMPLETED ASC, $COL_TASK_DUE ASC, $COL_TASK_CREATED DESC"
        )
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_TASK_ID)
            val titleIdx = it.getColumnIndexOrThrow(COL_TASK_TITLE)
            val notesIdx = it.getColumnIndexOrThrow(COL_TASK_NOTES)
            val dueIdx = it.getColumnIndexOrThrow(COL_TASK_DUE)
            val compIdx = it.getColumnIndexOrThrow(COL_TASK_COMPLETED)
            val prioIdx = it.getColumnIndexOrThrow(COL_TASK_PRIORITY)
            val createdIdx = it.getColumnIndexOrThrow(COL_TASK_CREATED)
            while (it.moveToNext()) {
                list.add(
                    JarvisTask(
                        id = it.getLong(idIdx),
                        title = it.getString(titleIdx),
                        notes = it.getString(notesIdx) ?: "",
                        dueTimestamp = if (it.isNull(dueIdx)) null else it.getLong(dueIdx),
                        isCompleted = it.getInt(compIdx) == 1,
                        priority = it.getInt(prioIdx),
                        createdAt = it.getLong(createdIdx)
                    )
                )
            }
        }
        list
    }

    suspend fun completeTask(titleOrId: String): Boolean = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COL_TASK_COMPLETED, 1)
        }
        val affected = if (titleOrId.toLongOrNull() != null) {
            writableDatabase.update(TABLE_TASKS, values, "$COL_TASK_ID = ?", arrayOf(titleOrId))
        } else {
            writableDatabase.update(TABLE_TASKS, values, "$COL_TASK_TITLE LIKE ?", arrayOf("%$titleOrId%"))
        }
        affected > 0
    }

    suspend fun deleteTask(id: Long): Int = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_TASKS, "$COL_TASK_ID = ?", arrayOf(id.toString()))
    }
}
