package com.akhilasdeveloper.asciicamera.repository.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FilterSpecsTable::class],
    version = 3
)
abstract class ASCIIDatabase : RoomDatabase() {
    abstract fun getFilterSpecsDao(): FilterSpecsDao
}