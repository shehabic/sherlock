package com.shehabic.sherlock.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context


@Database(entities = arrayOf(NetworkRequests::class, Sessions::class), version = 1, exportSchema = false)
abstract class Db : RoomDatabase() {
    abstract fun networkRequestsDao(): DaoAccess

    companion object {
        private var INSTANCE: Db? = null

        fun getInstance(context: Context): Db? {
            if (INSTANCE == null) {
                synchronized(Db::class) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                        Db::class.java, "com.shehabic.sherlock.network_requests.db")
                        .build()
                }
            }
            return INSTANCE
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }

}
