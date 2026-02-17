package com.kaii.photos.mediastore

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import androidx.core.net.toUri

class LavenderContentProvider : ContentProvider() {
    companion object {
        private const val AUTHORITY = "com.kaii.photos.content_provider"

        private const val TABLE_NAME = "custom_albums"
        val CONTENT_URI: Uri = "content://$AUTHORITY/$TABLE_NAME".toUri()
    }

    private lateinit var database: SQLiteDatabase
    private val dbName = "album_db"
    private val dbVersion = 3
    private val createSQLTable = " CREATE TABLE " + TABLE_NAME +
            " (${LavenderMediaColumns.ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
            " ${LavenderMediaColumns.PARENT_ID} INTEGER NOT NULL," +
            " ${LavenderMediaColumns.URI} TEXT NOT NULL," +
            " ${LavenderMediaColumns.MIME_TYPE} TEXT NOT NULL," +
            "${LavenderMediaColumns.DATE_TAKEN} INTEGER NOT NULL);"

    inner class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, dbName, null, dbVersion) {
        override fun onCreate(db: SQLiteDatabase?) {
            db?.execSQL(createSQLTable)
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db)
        }
    }

    override fun onCreate(): Boolean {
        if (context == null) return false

        val dbHelper = DatabaseHelper(context = context!!)
        database = dbHelper.writableDatabase

        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val qb = SQLiteQueryBuilder()
        qb.tables = TABLE_NAME

        val nonNullSortOrder = if (sortOrder == null || sortOrder == "") "id" else sortOrder

        val cursor = qb.query(database, projection, selection, selectionArgs, null, null, nonNullSortOrder)
        cursor.setNotificationUri(context!!.contentResolver, uri)

        return cursor
    }

    override fun getType(uri: Uri): String? {
        query(
            uri = CONTENT_URI,
            projection = arrayOf(MediaColumns.MIME_TYPE),
            selection = "${MediaColumns.MIME_TYPE} = ${uri.lastPathSegment}",
            selectionArgs = null,
            sortOrder = null
        ).use { cursor ->
            if (cursor == null) return null

            cursor.moveToFirst()
            return cursor.getString(cursor.getColumnIndexOrThrow("mimetype"))
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val rowId = database.insert(TABLE_NAME, "", values)

        if (rowId > 0 && context != null){
            val appendedUri = ContentUris.withAppendedId(CONTENT_URI, rowId)
            context!!.contentResolver.notifyChange(appendedUri, null)
            return appendedUri
        }

        throw SQLException("Failed to add a record into $uri")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val rowId = database.delete(TABLE_NAME, selection, selectionArgs)
        context!!.contentResolver.notifyChange(uri, null)
        return rowId
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        delete(uri, selection, selectionArgs)
        val rowId = database.insert(TABLE_NAME, "", values)

        if (rowId > 0 && context != null) {
            val appendedUri = ContentUris.withAppendedId(CONTENT_URI, rowId)
            context!!.contentResolver.notifyChange(appendedUri, null)
            return rowId.toInt()
        }

        throw SQLException("Failed to update $uri")
    }
}

object LavenderMediaColumns {
    const val ID = "id"
    const val PARENT_ID = "parentId"
    const val URI = "uri"
    const val MIME_TYPE = "mimetype"
    const val DATE_TAKEN = "date_taken"
}
