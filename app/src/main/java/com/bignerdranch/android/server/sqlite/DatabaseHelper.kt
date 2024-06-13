package com.bignerdranch.android.server.sqlite

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_TABLE_USERS)
        db?.execSQL(CREATE_TABLE_REQUESTS)
        db?.execSQL(CREATE_TABLE_STATUS)
        insertDefaultStatuses(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_REQUESTS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_STATUS")
        onCreate(db)
    }

    private fun insertDefaultStatuses(db: SQLiteDatabase?) {
        val statuses = listOf(
            0 to "User disconnect",
            1 to "OK",
            2 to "Error"
        )
        statuses.forEach { (id, description) ->
            val values = ContentValues().apply {
                put(COLUMN_STATUS_ID, id)
                put(COLUMN_STATUS_DESCRIPTION, description)
            }
            db?.insert(TABLE_STATUS, null, values)
        }
    }

    fun getRequestDetailsAsString(requestId: Long): String {
        val db = this.readableDatabase
        val requestProjection = arrayOf(
            COLUMN_ID_REQUEST,
            COLUMN_USER_IP,
            COLUMN_PORT,
            COLUMN_SWIPE_DIRECTION,
            COLUMN_SWIPE_DISTANCE,
            COLUMN_REQUEST_STATUS
        )
        val requestSelection = "$COLUMN_ID_REQUEST = ?"
        val requestSelectionArgs = arrayOf(requestId.toString())

        val cursor = db.query(
            TABLE_REQUESTS,
            requestProjection,
            requestSelection,
            requestSelectionArgs,
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                val statusId = it.getInt(it.getColumnIndexOrThrow(COLUMN_REQUEST_STATUS))
                val statusDescription = getStatusDescription(statusId)

                return "Request ID: ${it.getLong(it.getColumnIndexOrThrow(COLUMN_ID_REQUEST))}:\n" +
                        "User IP: ${it.getString(it.getColumnIndexOrThrow(COLUMN_USER_IP))}, " +
                        "Port: ${it.getInt(it.getColumnIndexOrThrow(COLUMN_PORT))}, \n" +
                        "Swipe Direction: ${it.getInt(it.getColumnIndexOrThrow(COLUMN_SWIPE_DIRECTION))}, " +
                        "Swipe Distance: ${it.getInt(it.getColumnIndexOrThrow(COLUMN_SWIPE_DISTANCE))}, \n" +
                        "Request Status: $statusDescription"
            }
        }
        return ""
    }

    fun getPendingRequests(ipAddress: String): List<String> {
        val db = this.readableDatabase

        val query = """
        SELECT * FROM $TABLE_REQUESTS 
        WHERE $COLUMN_USER_IP = ?
        ORDER BY $COLUMN_ID_REQUEST DESC
    """
        val cursor = db.rawQuery(query, arrayOf(ipAddress))
        val requestDescriptions = mutableListOf<String>()

        var collect = true
        while (cursor.moveToNext() && collect) {
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REQUEST_STATUS))
            if (status == 1 || status == 2) {
                collect = false
            } else if (status == 0) {
                val requestId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID_REQUEST))
                val userIp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_IP))
                val port = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PORT))
                val swipeDirection = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SWIPE_DIRECTION))
                val swipeDistance = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SWIPE_DISTANCE))
                val statusDescription = getStatusDescription(status)

                val requestDescription = """
                Request ID: $requestId:
                User IP: $userIp,
                Port: $port,
                Swipe Direction: $swipeDirection,
                Swipe Distance: $swipeDistance,
                Request Status: $statusDescription
            """.trimIndent()
                requestDescriptions.add(requestDescription)
            }
        }
        cursor.close()

        return requestDescriptions.reversed()
    }



    private fun getStatusDescription(statusId: Int): String {
        val db = this.readableDatabase
        val statusProjection = arrayOf(COLUMN_STATUS_DESCRIPTION)
        val statusSelection = "$COLUMN_STATUS_ID = ?"
        val statusSelectionArgs = arrayOf(statusId.toString())

        val cursor = db.query(
            TABLE_STATUS,
            statusProjection,
            statusSelection,
            statusSelectionArgs,
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(COLUMN_STATUS_DESCRIPTION))
            }
        }
        return "Unknown"
    }


    fun updateRequestStatus(requestId: Long, status: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_REQUEST_STATUS, status)
        }
        db.update(TABLE_REQUESTS, values, "$COLUMN_ID_REQUEST = ?", arrayOf(requestId.toString()))
    }

    fun insertLinkData(ipAddress: String, port: Int, swipeDirection: Int, swipeDistance: Int, requestStatus: Int): Long {
        val db = this.writableDatabase
        val linkValues = ContentValues().apply {
            put(COLUMN_USER_IP, ipAddress)
            put(COLUMN_PORT, port)
            put(COLUMN_SWIPE_DIRECTION, swipeDirection)
            put(COLUMN_SWIPE_DISTANCE, swipeDistance)
            put(COLUMN_REQUEST_STATUS, requestStatus)
        }

        return db.insert(TABLE_REQUESTS, null, linkValues)
    }

    fun isIpInDatabase(ip: String): Boolean {
        val db = this.readableDatabase
        val cursor: Cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_IP_ADDRESS),
            "$COLUMN_IP_ADDRESS = ?",
            arrayOf(ip),
            null,
            null,
            null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun insertIpAddress(ip: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IP_ADDRESS, ip)
        }
        db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    companion object {
        private const val DATABASE_NAME = "mydatabase.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_USERS = "users"
        const val COLUMN_IP_ADDRESS = "ip_address"

        const val TABLE_REQUESTS = "requests"
        const val COLUMN_ID_REQUEST = "id_request"
        const val COLUMN_PORT = "port"
        const val COLUMN_SWIPE_DIRECTION = "swipe_direction"
        const val COLUMN_SWIPE_DISTANCE = "swipe_distance"
        const val COLUMN_REQUEST_STATUS = "request_status"
        const val COLUMN_USER_IP = "user_ip"

        const val TABLE_STATUS = "status"
        const val COLUMN_STATUS_ID = "status_id"
        const val COLUMN_STATUS_DESCRIPTION = "status_description"

        private const val CREATE_TABLE_USERS = "CREATE TABLE $TABLE_USERS (" +
                "$COLUMN_IP_ADDRESS TEXT PRIMARY KEY)"

        private const val CREATE_TABLE_REQUESTS = "CREATE TABLE $TABLE_REQUESTS (" +
                "$COLUMN_ID_REQUEST INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_USER_IP TEXT," +
                "$COLUMN_PORT INTEGER," +
                "$COLUMN_SWIPE_DIRECTION INTEGER," +
                "$COLUMN_SWIPE_DISTANCE INTEGER," +
                "$COLUMN_REQUEST_STATUS INTEGER," +
                "FOREIGN KEY($COLUMN_USER_IP) REFERENCES $TABLE_USERS($COLUMN_IP_ADDRESS) ON DELETE CASCADE," +
                "FOREIGN KEY($COLUMN_REQUEST_STATUS) REFERENCES $TABLE_STATUS($COLUMN_STATUS_ID) ON DELETE CASCADE)"

        private const val CREATE_TABLE_STATUS = "CREATE TABLE $TABLE_STATUS (" +
                "$COLUMN_STATUS_ID INTEGER PRIMARY KEY," +
                "$COLUMN_STATUS_DESCRIPTION TEXT)"
    }
}
