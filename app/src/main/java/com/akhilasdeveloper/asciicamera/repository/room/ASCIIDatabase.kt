package com.akhilasdeveloper.asciicamera.repository.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FilterSpecsTable::class, FilterSpecsDownloadsTable::class],
    version = 2
)
abstract class ASCIIDatabase : RoomDatabase() {
    abstract fun getFilterSpecsDao(): FilterSpecsDao
}