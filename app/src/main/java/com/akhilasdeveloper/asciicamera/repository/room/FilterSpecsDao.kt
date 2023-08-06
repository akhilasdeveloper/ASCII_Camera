package com.akhilasdeveloper.asciicamera.repository.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterSpecsDao {
    @Query("SELECT * FROM filter_specs_table WHERE isDownloaded = 0")
    fun getFilters(): Flow<List<FilterSpecsTable>>

    @Query("SELECT * from filter_specs_table where id = :id")
    fun getFilterById(id: Int): FilterSpecsTable?

    @Query("SELECT count(*) from filter_specs_table WHERE isDownloaded = 0")
    fun getFiltersCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addFilter(filterSpecs: FilterSpecsTable)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addFilters(filterSpecs: List<FilterSpecsTable>)

    @Delete
    fun deleteFilter(filterSpecs: FilterSpecsTable)

    @Query("SELECT * FROM filter_specs_table WHERE isDownloaded = 1")
    fun getDownloadedFilters(): Flow<List<FilterSpecsTable>>

    @Query("DELETE FROM filter_specs_table WHERE isDownloaded = 1")
    fun deleteAllDownloads(): Int

}