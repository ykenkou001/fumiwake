package dev.fumiwake.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import dev.fumiwake.core.progress.ProgressStore

/**
 * [ProgressStore] backed by Android's built-in SQLite ([SQLiteOpenHelper], no external
 * library — docs/IMPLEMENTATION_PLAN.md §3/§9: user progress is the only thing this app
 * persists to disk; the exercise catalog itself is re-read from assets every launch).
 *
 * Two tables, exactly as specified in the M2b task brief:
 *  - `completed_exercises(exercise_id TEXT PRIMARY KEY)`
 *  - `learned_concepts(concept_id TEXT PRIMARY KEY)`
 */
class SqliteProgressStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION),
    ProgressStore {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_COMPLETED (exercise_id TEXT PRIMARY KEY)")
        db.execSQL("CREATE TABLE $TABLE_LEARNED (concept_id TEXT PRIMARY KEY)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMPLETED")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEARNED")
        onCreate(db)
    }

    override fun completedExercises(): Set<String> = readColumn(TABLE_COMPLETED, "exercise_id")

    override fun markCompleted(exerciseId: String) {
        insertIgnore(TABLE_COMPLETED, "exercise_id", exerciseId)
    }

    override fun learnedConcepts(): Set<String> = readColumn(TABLE_LEARNED, "concept_id")

    override fun markConceptLearned(conceptId: String) {
        insertIgnore(TABLE_LEARNED, "concept_id", conceptId)
    }

    private fun readColumn(table: String, column: String): Set<String> {
        val result = mutableSetOf<String>()
        readableDatabase.query(table, arrayOf(column), null, null, null, null, null).use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(column)
            while (cursor.moveToNext()) {
                result.add(cursor.getString(columnIndex))
            }
        }
        return result
    }

    private fun insertIgnore(table: String, column: String, value: String) {
        val values = ContentValues().apply { put(column, value) }
        writableDatabase.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    companion object {
        private const val DB_NAME = "fumiwake_progress.db"
        private const val DB_VERSION = 1
        private const val TABLE_COMPLETED = "completed_exercises"
        private const val TABLE_LEARNED = "learned_concepts"
    }
}
